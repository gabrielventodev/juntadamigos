package com.juntadamigos.core.google

import com.juntadamigos.core.MeetingPointFinder
import com.juntadamigos.core.Participant
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.TravelMode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Prueba contra las APIs reales de Google. Cuesta plata: solo corre con
 *   LIVE_GOOGLE_TESTS=1 GOOGLE_MAPS_API_KEY=... ./gradlew :core:test --tests '*LiveGoogleSmokeTest*'
 */
class LiveGoogleSmokeTest {
    private val key: String? = System.getenv("GOOGLE_MAPS_API_KEY")

    private fun requireLive() {
        assumeTrue(System.getenv("LIVE_GOOGLE_TESTS") == "1" && !key.isNullOrBlank(), "Live tests desactivados")
    }

    @Test
    fun `geocoding real`() = runBlocking {
        requireLive()
        val loc = GoogleGeocoder(key!!, region = "ar").geocode("Av. Corrientes 1500, Buenos Aires")
        assertNotNull(loc)
        println("[live] geocoding Corrientes 1500 -> $loc")
    }

    @Test
    fun `autocompletado y detalles reales`() = runBlocking {
        requireLive()
        val ac = GooglePlaceAutocomplete(key!!, regionCodes = listOf("ar"))
        val token = UUID.randomUUID().toString()
        val suggestions = ac.suggest("Av Cabildo 2000", token)
        println("[live] autocomplete -> ${suggestions.take(3)}")
        assertTrue(suggestions.isNotEmpty())
        val resolved = ac.resolve(suggestions.first().placeId, token)
        println("[live] details -> $resolved")
        assertNotNull(resolved)
    }

    @Test
    fun `punto de encuentro real con modos mezclados`() = runBlocking {
        requireLive()
        val geocoder = GoogleGeocoder(key!!, region = "ar")
        val people = listOf(
            Triple("Gabriel", "Av. Corrientes 1500, CABA", TravelMode.DRIVE),
            Triple("Ana", "Av. Cabildo 2000, CABA", TravelMode.TRANSIT),
            Triple("Beto", "Av. Rivadavia 6000, CABA", TravelMode.TRANSIT),
            Triple("Caro", "Av. Santa Fe 3200, CABA", TravelMode.BICYCLE),
        ).map { (name, address, mode) ->
            Participant(name.lowercase(), name, assertNotNull(geocoder.geocode(address), address), mode)
        }
        val finder = MeetingPointFinder(GooglePlaceSearch(key), GoogleRouteMatrix(key))
        for (category in listOf(PlaceCategory.CAFE, PlaceCategory.BAR)) {
            val result = finder.find(people, category, maxIndividualTravel = 60.minutes)
            println("[live] $category centro=${result.searchCenter} radio=${result.searchRadiusMeters.toInt()}m " +
                "evaluados=${result.candidatesEvaluated} sinRuta=${result.discardedUnreachable} sobreTope=${result.discardedOverCap}")
            result.suggestions.forEach { s ->
                println("[live]   ${s.place.name} (${s.place.address}) total=${s.totalTravelTime} peor=${s.maxTravelTime} ${s.travelTimes}")
            }
            assertTrue(result.candidatesEvaluated > 0, "Places no devolvió candidatos para $category")
            assertTrue(result.suggestions.isNotEmpty(), "Ningún candidato sirvió para $category")
        }
    }
}
