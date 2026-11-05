package com.juntadamigos.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Elige el lugar real más parejo para el grupo, con prioridad para quien va sin auto.
 *
 * Criterio (decidido por Gabriel el 2026-11-04, ver [rankKey]):
 * 1. Quien va sin auto no viaja más de su tope ([Config.noCarMaxTravel]): 20 min en transporte
 *    o bici, 35 min a pie (20 min a pie son ~1,6 km y no alcanza para emparejar con un auto en tráfico).
 * 2. Entre los lugares que cumplen eso, gana el de menor peor viaje: todos llegan parecido.
 * 3. Desempata el menor tiempo total y después el rating.
 * Respaldo si ningún lugar cumple el punto 1: gana el que menos se pasa de su tope para
 * quien va sin auto (en escalones de [Config.noCarFallbackStep]) y, dentro del escalón, el más parejo.
 *
 * Algoritmo:
 * 1. Arma puntos de muestra: el centro geográfico y puntos sobre el segmento entre el centro
 *    y cada participante (incluida su propia ubicación).
 * 2. Pide los tiempos reales de todos a esos puntos y se queda con las [Config.searchAreas]
 *    zonas que mejor cumplen el criterio.
 * 3. Busca lugares de la categoría elegida alrededor de esas zonas.
 * 4. Para cada modo de viaje, pide una matriz de tiempos (participantes con ese modo × candidatos).
 * 5. Descarta candidatos donde alguien no tiene ruta o donde alguien supera el tope individual.
 * 6. Ordena con el criterio de arriba.
 *
 * Historia: "menor tiempo total" dejaba al de auto en 1 h y al de colectivo en 5 min (o al
 * revés, según dónde se buscaba). Probado en el celular de Gabriel: Jose a pie 4 min,
 * Gabriel en auto 51 min. De ahí el criterio parejo con tope para el que va sin auto.
 */
class MeetingPointFinder(
    private val placeSearch: PlaceSearch,
    private val travelTimeMatrix: TravelTimeMatrix,
    private val config: Config = Config(),
) {
    data class Config(
        /** Tope por modo para quien va sin auto. DRIVE no tiene tope. */
        val noCarMaxTravel: Map<TravelMode, Duration> = mapOf(
            TravelMode.TRANSIT to 20.minutes,
            TravelMode.BICYCLE to 20.minutes,
            TravelMode.WALK to 35.minutes,
        ),
        /** Si nadie cumple el tope, pasarse 0-5 min cuenta igual, 5-10 peor, etc. */
        val noCarFallbackStep: Duration = 5.minutes,
        /** Cantidad de candidatos a evaluar. Más candidatos = mejor resultado y más costo de API. */
        val maxCandidates: Int = 15,
        /** Puntos de muestra entre el centro y cada participante (el último es su ubicación). */
        val samplesPerParticipant: Int = 4,
        /** En cuántas de las mejores zonas muestreadas se buscan lugares. */
        val searchAreas: Int = 3,
        val minRadiusMeters: Double = 1_000.0,
        /** Límite de la Places API (New) para searchNearby. */
        val maxRadiusMeters: Double = 50_000.0,
        /**
         * Elementos máximos por llamada a la matriz. Routes API admite 625 en general
         * y 100 con TRANSIT; usamos 100 para todos y partimos en lotes.
         */
        val maxMatrixElements: Int = 100,
    ) {
        init {
            require(noCarFallbackStep.isPositive())
            require(TravelMode.entries.filter { it != TravelMode.DRIVE }.all { noCarMaxTravel[it]?.isPositive() == true }) {
                "Falta el tope de algún modo sin auto"
            }
            require(maxCandidates > 0)
            require(samplesPerParticipant > 0)
            require(searchAreas > 0)
            require(minRadiusMeters in 1.0..maxRadiusMeters)
            require(maxMatrixElements > 0)
        }
    }

    suspend fun find(
        participants: List<Participant>,
        category: PlaceCategory,
        /** Tope opcional: ningún participante puede viajar más que esto. */
        maxIndividualTravel: Duration? = null,
        topN: Int = 3,
    ): MeetingResult {
        require(participants.size >= 2) { "Se necesitan al menos 2 participantes" }
        require(participants.map { it.id }.toSet().size == participants.size) { "Ids de participantes repetidos" }

        val center = Geo.centroid(participants.map { it.location })
        val radius = searchRadius(center, participants)
        val areas = bestAreas(participants, center, radius, maxIndividualTravel)
        val perArea = (config.maxCandidates + areas.size - 1) / areas.size
        val candidates = coroutineScope {
            areas.map { a -> async { placeSearch.searchNearby(a.center, a.radiusMeters, category, perArea) } }
                .awaitAll()
        }
            // Intercalar las zonas para que el recorte no deje afuera a la segunda.
            .let { lists -> (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } } }
            .distinctBy { it.id }
            .take(config.maxCandidates)

        if (candidates.isEmpty()) {
            return MeetingResult(emptyList(), areas, 0, 0, 0)
        }

        val times = travelTimesByParticipant(participants, candidates.map { it.location })

        var unreachable = 0
        var overCap = 0
        val suggestions = candidates.mapIndexedNotNull { j, place ->
            val perPerson = participants.associate { p -> p.id to times.getValue(p.id)[j] }
            if (perPerson.values.any { it == null }) {
                unreachable++
                return@mapIndexedNotNull null
            }
            @Suppress("UNCHECKED_CAST")
            val real = perPerson as Map<String, Duration>
            val suggestion = Suggestion(place, real)
            if (maxIndividualTravel != null && suggestion.maxTravelTime > maxIndividualTravel) {
                overCap++
                return@mapIndexedNotNull null
            }
            suggestion
        }

        val ranked = suggestions.sortedWith(
            compareBy<Suggestion> { rankKey(participants, it.travelTimes) }
                .thenByDescending { it.place.rating ?: 0.0 },
        )

        return MeetingResult(
            suggestions = ranked.take(topN),
            searchAreas = areas,
            candidatesEvaluated = candidates.size,
            discardedUnreachable = unreachable,
            discardedOverCap = overCap,
        )
    }

    /** Orden de un lugar: primero cuánto se pasa del tope quien va sin auto, después el peor viaje, después el total. */
    internal data class RankKey(val noCarOverSteps: Long, val maxMinutes: Long, val totalSeconds: Long) : Comparable<RankKey> {
        override fun compareTo(other: RankKey): Int =
            compareValuesBy(this, other, { it.noCarOverSteps }, { it.maxMinutes }, { it.totalSeconds })
    }

    internal fun rankKey(participants: List<Participant>, times: Map<String, Duration>): RankKey {
        val over = participants.filter { it.mode != TravelMode.DRIVE }
            .maxOfOrNull { times.getValue(it.id) - config.noCarMaxTravel.getValue(it.mode) } ?: Duration.ZERO
        val steps = if (!over.isPositive()) 0L else
            (over.inWholeSeconds + config.noCarFallbackStep.inWholeSeconds - 1) / config.noCarFallbackStep.inWholeSeconds
        val max = times.values.max()
        // Peor viaje redondeado al minuto: 1 min de diferencia no hace más justo un lugar; desempata el total.
        val total = times.values.fold(Duration.ZERO) { acc, d -> acc + d }
        return RankKey(steps, (max.inWholeSeconds + 30) / 60, total.inWholeSeconds)
    }

    /** Radio de cada zona: el paso entre puntos de muestra, para cubrir el hueco entre ellos. */
    internal fun searchRadius(center: LatLng, participants: List<Participant>): Double {
        val farthest = participants.maxOf { Geo.distanceMeters(center, it.location) }
        return (farthest / config.samplesPerParticipant).coerceIn(config.minRadiusMeters, config.maxRadiusMeters)
    }

    internal fun samplePoints(center: LatLng, participants: List<Participant>): List<LatLng> {
        val n = config.samplesPerParticipant
        return listOf(center) + participants.flatMap { p ->
            (1..n).map { k -> Geo.interpolate(center, p.location, k.toDouble() / n) }
        }
    }

    /**
     * Mide tiempos reales a los puntos de muestra y devuelve las mejores zonas, separadas
     * entre sí al menos un radio. Si ningún punto tiene ruta para todos, busca en el centro.
     */
    private suspend fun bestAreas(
        participants: List<Participant>,
        center: LatLng,
        radius: Double,
        maxIndividualTravel: Duration?,
    ): List<SearchArea> {
        val samples = samplePoints(center, participants)
        val times = travelTimesByParticipant(participants, samples)
        val scored = samples.indices.mapNotNull { j ->
            val perPerson = participants.associate { p -> p.id to (times.getValue(p.id)[j] ?: return@mapNotNull null) }
            // Con tope individual, preferir zonas que lo cumplen; las demás quedan como último recurso.
            val overCap = maxIndividualTravel != null && perPerson.values.max() > maxIndividualTravel
            Triple(samples[j], overCap, rankKey(participants, perPerson))
        }.sortedWith(compareBy<Triple<LatLng, Boolean, RankKey>> { it.second }.thenBy { it.third })

        val chosen = mutableListOf<LatLng>()
        for ((point, _, _) in scored) {
            if (chosen.size == config.searchAreas) break
            if (chosen.none { Geo.distanceMeters(it, point) < radius }) chosen += point
        }
        if (chosen.isEmpty()) chosen += center
        return chosen.map { SearchArea(it, radius) }
    }

    /** Devuelve, por id de participante, la lista de tiempos a cada candidato (mismo orden). */
    private suspend fun travelTimesByParticipant(
        participants: List<Participant>,
        destinations: List<LatLng>,
    ): Map<String, List<Duration?>> = coroutineScope {
        participants.groupBy { it.mode }.map { (mode, group) ->
            async {
                val rows = matrixInBatches(group.map { it.location }, destinations, mode)
                group.mapIndexed { i, p -> p.id to rows[i] }
            }
        }.awaitAll().flatten().toMap()
    }

    /** Parte la matriz para respetar el límite de elementos por llamada. */
    private suspend fun matrixInBatches(
        origins: List<LatLng>,
        destinations: List<LatLng>,
        mode: TravelMode,
    ): List<List<Duration?>> {
        val destPerBatch = minOf(destinations.size, config.maxMatrixElements).coerceAtLeast(1)
        val originsPerBatch = (config.maxMatrixElements / destPerBatch).coerceAtLeast(1)
        val result = List(origins.size) { arrayOfNulls<Duration>(destinations.size) }

        for (oStart in origins.indices step originsPerBatch) {
            val oChunk = origins.subList(oStart, minOf(oStart + originsPerBatch, origins.size))
            for (dStart in destinations.indices step destPerBatch) {
                val dChunk = destinations.subList(dStart, minOf(dStart + destPerBatch, destinations.size))
                val rows = travelTimeMatrix.travelTimes(oChunk, dChunk, mode)
                check(rows.size == oChunk.size && rows.all { it.size == dChunk.size }) {
                    "La matriz devuelta no coincide con ${oChunk.size}x${dChunk.size}"
                }
                rows.forEachIndexed { i, row ->
                    row.forEachIndexed { j, d -> result[oStart + i][dStart + j] = d }
                }
            }
        }
        return result.map { it.toList() }
    }
}
