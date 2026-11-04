package com.juntadamigos.core

import java.net.URLEncoder
import kotlin.time.Duration

/** Textos y links para compartir un resultado. Sin dependencias de Android para poder testearlo. */
object MeetingShare {

    /** "12 min" o "1 h 05 min", redondeado al minuto. */
    fun formatDuration(d: Duration): String {
        val minutes = (d.inWholeSeconds + 30) / 60
        return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${"%02d".format(minutes % 60)} min"
    }

    /**
     * Link universal de Google Maps: abre la app si está instalada y el navegador si no.
     * https://developers.google.com/maps/documentation/urls/get-started#search-action
     */
    fun mapsUrl(place: Place): String {
        val query = URLEncoder.encode(listOfNotNull(place.name, place.address).joinToString(", "), "UTF-8")
        return "https://www.google.com/maps/search/?api=1&query=$query&query_place_id=${URLEncoder.encode(place.id, "UTF-8")}"
    }

    /** Mensaje para WhatsApp: lugar, dirección, link y cuánto tarda cada uno (el que más tarda primero). */
    fun message(suggestion: Suggestion, names: Map<String, String>): String = buildString {
        appendLine("¿Nos juntamos en ${suggestion.place.name}?")
        suggestion.place.address?.let { appendLine(it) }
        appendLine(mapsUrl(suggestion.place))
        appendLine()
        appendLine("Lo que tarda cada uno:")
        suggestion.travelTimes.entries.sortedByDescending { it.value }.forEach { (id, d) ->
            appendLine("• ${names[id] ?: id}: ${formatDuration(d)}")
        }
        append("Calculado con Juntadamigos")
    }
}
