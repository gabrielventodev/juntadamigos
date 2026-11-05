package com.juntadamigos.app.ui

import com.juntadamigos.core.LatLng
import com.juntadamigos.core.Place
import com.juntadamigos.core.Suggestion
import com.juntadamigos.core.TravelMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class FairnessTest {
    private val place = Place("p", "La Pérgola", LatLng(-34.6, -58.4))

    private fun suggestion(vararg times: Pair<String, Int>) =
        Suggestion(place, times.associate { (id, m) -> id to m.minutes })

    @Test
    fun parejoHastaCincoMinutos() {
        val f = fairnessOf(suggestion("a" to 14, "b" to 17, "c" to 15), mapOf("a" to TravelMode.DRIVE, "b" to TravelMode.TRANSIT, "c" to TravelMode.BICYCLE))
        assertEquals(Evenness.EVEN, f.evenness)
        assertEquals(3, f.spreadMinutes)
    }

    @Test
    fun medioParejoEntreSeisYDiez() {
        val f = fairnessOf(suggestion("a" to 10, "b" to 19), mapOf("a" to TravelMode.DRIVE, "b" to TravelMode.TRANSIT))
        assertEquals(Evenness.MEDIUM, f.evenness)
    }

    @Test
    fun sinAutoPasadoDelTopeEsDesparejoAunqueLaDiferenciaSeaChica() {
        // Bici 23 > tope 20, auto 20: 3 min de diferencia pero alguien sin auto se pasa.
        val f = fairnessOf(suggestion("a" to 20, "b" to 23), mapOf("a" to TravelMode.DRIVE, "b" to TravelMode.BICYCLE))
        assertEquals(Evenness.UNEVEN, f.evenness)
        assertEquals(listOf(OverCap("b", 23, 20)), f.overCap)
    }

    @Test
    fun aPieTieneTopeDe35() {
        val f = fairnessOf(suggestion("a" to 30, "b" to 33), mapOf("a" to TravelMode.DRIVE, "b" to TravelMode.WALK))
        assertEquals(Evenness.EVEN, f.evenness)
        assertEquals(emptyList(), f.overCap)
    }
}
