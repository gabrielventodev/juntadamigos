package com.juntadamigos.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Lugares fijos, ignora el radio. */
private class FakePlaces(private val places: List<Place>) : PlaceSearch {
    override suspend fun searchNearby(center: LatLng, radiusMeters: Double, category: PlaceCategory, maxResults: Int) =
        places.take(maxResults)
}

/** Lugares fijos, devuelve solo los que caen dentro del círculo pedido. Registra las búsquedas. */
private class CirclePlaces(private val places: List<Place>) : PlaceSearch {
    val searches = mutableListOf<SearchArea>()
    override suspend fun searchNearby(center: LatLng, radiusMeters: Double, category: PlaceCategory, maxResults: Int): List<Place> {
        searches += SearchArea(center, radiusMeters)
        return places.filter { Geo.distanceMeters(center, it.location) <= radiusMeters }.take(maxResults)
    }
}

/** Tiempo = distancia en línea recta / velocidad del modo. Cuenta llamadas y elementos. */
private class FakeMatrix(
    private val speedsMps: Map<TravelMode, Double> = mapOf(
        TravelMode.DRIVE to 10.0,
        TravelMode.TRANSIT to 5.0,
        TravelMode.WALK to 1.4,
        TravelMode.BICYCLE to 4.0,
    ),
    private val unreachable: Set<Pair<TravelMode, LatLng>> = emptySet(),
) : TravelTimeMatrix {
    var calls = 0
    var maxElementsSeen = 0

    override suspend fun travelTimes(origins: List<LatLng>, destinations: List<LatLng>, mode: TravelMode): List<List<Duration?>> {
        calls++
        maxElementsSeen = maxOf(maxElementsSeen, origins.size * destinations.size)
        return origins.map { o ->
            destinations.map { d ->
                if ((mode to d) in unreachable) null
                else (Geo.distanceMeters(o, d) / speedsMps.getValue(mode)).seconds
            }
        }
    }
}

class MeetingPointFinderTest {
    // Puntos sobre el ecuador: 0.01° de longitud ≈ 1.11 km.
    private fun at(lon: Double) = LatLng(0.0, lon)
    private fun place(id: String, lon: Double, rating: Double? = null) = Place(id, id, at(lon), rating = rating)

    @Test
    fun `elige el lugar con menor tiempo total`() = runTest {
        val friends = listOf(
            Participant("a", "Ana", at(0.00), TravelMode.DRIVE),
            Participant("b", "Beto", at(0.10), TravelMode.DRIVE),
        )
        val places = listOf(place("lejos", 0.30), place("medio", 0.05), place("borde", 0.12))
        val result = MeetingPointFinder(FakePlaces(places), FakeMatrix()).find(friends, PlaceCategory.CAFE)

        // Entre dos personas, cualquier punto del segmento da el mismo total; "borde" queda afuera.
        assertEquals("medio", result.suggestions.first().place.id)
        assertEquals(listOf("medio", "borde", "lejos"), result.suggestions.map { it.place.id })
    }

    @Test
    fun `quien va mas lento arrastra el lugar hacia su lado`() = runTest {
        val friends = listOf(
            Participant("auto", "Con auto", at(0.00), TravelMode.DRIVE),
            Participant("bondi", "Sin auto", at(0.10), TravelMode.TRANSIT),
        )
        val places = listOf(place("cerca-auto", 0.02), place("medio", 0.05), place("cerca-bondi", 0.08))
        val result = MeetingPointFinder(FakePlaces(places), FakeMatrix()).find(friends, PlaceCategory.BAR)

        assertEquals("cerca-bondi", result.suggestions.first().place.id)
    }

    @Test
    fun `el tope individual evita castigar al que vive lejos`() = runTest {
        val friends = listOf(
            Participant("a", "A", at(0.00), TravelMode.TRANSIT),
            Participant("b", "B", at(0.01), TravelMode.TRANSIT),
            Participant("c", "C", at(0.02), TravelMode.TRANSIT),
            Participant("lejano", "Lejano", at(0.40), TravelMode.TRANSIT),
        )
        val places = listOf(place("grupo", 0.01), place("intermedio", 0.15))
        val finder = MeetingPointFinder(FakePlaces(places), FakeMatrix())

        val sinTope = finder.find(friends, PlaceCategory.CAFE)
        assertEquals("grupo", sinTope.suggestions.first().place.id)
        assertTrue(sinTope.suggestions.first().maxTravelTime > 120.minutes)

        val conTope = finder.find(friends, PlaceCategory.CAFE, maxIndividualTravel = 100.minutes)
        assertEquals(listOf("intermedio"), conTope.suggestions.map { it.place.id })
        assertEquals(1, conTope.discardedOverCap)
    }

    @Test
    fun `descarta lugares sin ruta para alguien`() = runTest {
        val friends = listOf(
            Participant("a", "A", at(0.00), TravelMode.DRIVE),
            Participant("b", "B", at(0.10), TravelMode.TRANSIT),
        )
        val sinTransporte = place("sin-bondi", 0.05)
        val places = listOf(sinTransporte, place("ok", 0.06))
        val matrix = FakeMatrix(unreachable = setOf(TravelMode.TRANSIT to sinTransporte.location))
        val result = MeetingPointFinder(FakePlaces(places), matrix).find(friends, PlaceCategory.CAFE)

        assertEquals(listOf("ok"), result.suggestions.map { it.place.id })
        assertEquals(1, result.discardedUnreachable)
    }

    @Test
    fun `parte la matriz en lotes respetando el limite`() = runTest {
        val friends = (0 until 7).map { Participant("p$it", "P$it", at(it * 0.01), TravelMode.TRANSIT) }
        val places = (0 until 15).map { place("l$it", it * 0.005) }
        val matrix = FakeMatrix()
        val config = MeetingPointFinder.Config(maxCandidates = 15, searchAreas = 1, maxMatrixElements = 20)
        val result = MeetingPointFinder(FakePlaces(places), matrix, config).find(friends, PlaceCategory.CAFE, topN = 15)

        assertTrue(matrix.maxElementsSeen <= 20, "lote de ${matrix.maxElementsSeen} elementos")
        assertEquals(15, result.suggestions.size)
        // Cada sugerencia tiene el tiempo de los 7 participantes.
        assertTrue(result.suggestions.all { it.travelTimes.size == 7 })
    }

    @Test
    fun `una llamada por modo de viaje cuando entra en un lote`() = runTest {
        val friends = listOf(
            Participant("a", "A", at(0.00), TravelMode.DRIVE),
            Participant("b", "B", at(0.02), TravelMode.DRIVE),
            Participant("c", "C", at(0.04), TravelMode.TRANSIT),
        )
        val matrix = FakeMatrix()
        MeetingPointFinder(FakePlaces(listOf(place("x", 0.02))), matrix).find(friends, PlaceCategory.CAFE)
        // 2 modos × (muestreo de zonas + candidatos).
        assertEquals(4, matrix.calls)
    }

    @Test
    fun `desempata por peor viaje y despues por rating`() = runTest {
        val friends = listOf(
            Participant("a", "A", at(0.00), TravelMode.DRIVE),
            Participant("b", "B", at(0.10), TravelMode.DRIVE),
        )
        val places = listOf(place("extremo", 0.00, rating = 5.0), place("centro-malo", 0.05, 3.0), place("centro-bueno", 0.05, 4.8))
        val result = MeetingPointFinder(FakePlaces(places), FakeMatrix()).find(friends, PlaceCategory.CAFE)
        assertEquals(listOf("centro-bueno", "centro-malo", "extremo"), result.suggestions.map { it.place.id })
    }

    @Test
    fun `sin candidatos devuelve lista vacia`() = runTest {
        val friends = listOf(Participant("a", "A", at(0.0), TravelMode.DRIVE), Participant("b", "B", at(0.1), TravelMode.DRIVE))
        val result = MeetingPointFinder(FakePlaces(emptyList()), FakeMatrix()).find(friends, PlaceCategory.PARK)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `exige al menos dos participantes`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            MeetingPointFinder(FakePlaces(emptyList()), FakeMatrix())
                .find(listOf(Participant("a", "A", at(0.0), TravelMode.DRIVE)), PlaceCategory.CAFE)
        }
    }

    @Test
    fun `el radio de busqueda queda entre el minimo y el maximo`() {
        val finder = MeetingPointFinder(FakePlaces(emptyList()), FakeMatrix())
        val cerca = listOf(Participant("a", "A", at(0.0), TravelMode.WALK), Participant("b", "B", at(0.001), TravelMode.WALK))
        val lejos = listOf(Participant("a", "A", at(0.0), TravelMode.DRIVE), Participant("b", "B", at(5.0), TravelMode.DRIVE))
        assertEquals(1_000.0, finder.searchRadius(Geo.centroid(cerca.map { it.location }), cerca))
        assertEquals(50_000.0, finder.searchRadius(Geo.centroid(lejos.map { it.location }), lejos))
    }

    @Test
    fun `busca lugares cerca de quien va a pie aunque esten lejos del centro`() = runTest {
        // Caso reportado: dos personas lejos, una en auto y otra a pie. El viejo algoritmo
        // solo buscaba alrededor del centro y nunca veía el café al lado del que camina.
        val friends = listOf(
            Participant("auto", "Con auto", at(0.00), TravelMode.DRIVE),
            Participant("pie", "A pie", at(0.10), TravelMode.WALK),
        )
        val places = listOf(place("centro", 0.05), place("cerca-auto", 0.005), place("cerca-pie", 0.098))
        val search = CirclePlaces(places)
        val result = MeetingPointFinder(search, FakeMatrix()).find(friends, PlaceCategory.CAFE)

        val best = result.suggestions.first()
        assertEquals("cerca-pie", best.place.id)
        assertTrue(best.travelTimes.getValue("pie") < best.travelTimes.getValue("auto"))
        assertTrue(search.searches.none { Geo.distanceMeters(it.center, at(0.05)) < 100 }, "no debería buscar en el centro")
    }

    @Test
    fun `con tope prefiere zonas que lo cumplen`() = runTest {
        val friends = listOf(
            Participant("auto", "Con auto", at(0.00), TravelMode.DRIVE),
            Participant("pie", "A pie", at(0.10), TravelMode.WALK),
        )
        val places = listOf(place("centro", 0.05), place("cerca-pie", 0.098), place("lado-pie", 0.08))
        val result = MeetingPointFinder(CirclePlaces(places), FakeMatrix())
            .find(friends, PlaceCategory.CAFE, maxIndividualTravel = 40.minutes)
        assertTrue(result.suggestions.isNotEmpty())
        assertTrue(result.suggestions.all { it.maxTravelTime <= 40.minutes })
    }

    @Test
    fun `los puntos de muestra incluyen el centro y la ubicacion de cada uno`() {
        val friends = listOf(
            Participant("a", "A", at(0.00), TravelMode.DRIVE),
            Participant("b", "B", at(0.10), TravelMode.WALK),
        )
        val finder = MeetingPointFinder(FakePlaces(emptyList()), FakeMatrix())
        val center = Geo.centroid(friends.map { it.location })
        val samples = finder.samplePoints(center, friends)
        assertEquals(1 + 2 * 4, samples.size)
        assertEquals(center, samples.first())
        friends.forEach { f -> assertTrue(samples.any { Geo.distanceMeters(it, f.location) < 1 }) }
    }
}
