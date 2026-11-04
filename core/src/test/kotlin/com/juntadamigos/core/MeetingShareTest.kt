package com.juntadamigos.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MeetingShareTest {
    private val place = Place("ChIJ_abc", "Café Tortoni", LatLng(-34.6, -58.4), address = "Av. de Mayo 825, CABA")

    @Test
    fun `formatea minutos y horas`() {
        assertEquals("0 min", MeetingShare.formatDuration(10.seconds))
        assertEquals("1 min", MeetingShare.formatDuration(31.seconds))
        assertEquals("59 min", MeetingShare.formatDuration(59.minutes))
        assertEquals("1 h 05 min", MeetingShare.formatDuration(65.minutes))
    }

    @Test
    fun `link de maps con nombre direccion y place id codificados`() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=Caf%C3%A9+Tortoni%2C+Av.+de+Mayo+825%2C+CABA&query_place_id=ChIJ_abc",
            MeetingShare.mapsUrl(place),
        )
    }

    @Test
    fun `mensaje con lugar link y tiempos de mayor a menor`() {
        val s = Suggestion(place, mapOf("me" to 10.minutes, "f1" to 70.minutes))
        val text = MeetingShare.message(s, mapOf("me" to "Gabriel", "f1" to "Ana"))
        val lines = text.lines()
        assertEquals("¿Nos juntamos en Café Tortoni?", lines[0])
        assertEquals("Av. de Mayo 825, CABA", lines[1])
        assertTrue(lines[2].startsWith("https://www.google.com/maps/"))
        assertTrue(text.indexOf("Ana: 1 h 10 min") < text.indexOf("Gabriel: 10 min"))
    }
}
