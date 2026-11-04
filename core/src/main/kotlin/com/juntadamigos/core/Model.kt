package com.juntadamigos.core

import kotlin.time.Duration

/** Coordenada geográfica en grados decimales. */
data class LatLng(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitud fuera de rango: $latitude" }
        require(longitude in -180.0..180.0) { "Longitud fuera de rango: $longitude" }
    }
}

/** Cómo viaja cada participante. Define qué matriz de tiempos se le calcula. */
enum class TravelMode { DRIVE, TRANSIT, WALK, BICYCLE }

/** Una persona que va al encuentro (incluye al dueño de la app). */
data class Participant(
    val id: String,
    val name: String,
    val location: LatLng,
    val mode: TravelMode,
)

/**
 * Tipo de lugar buscado, en tipos de la Places API (New):
 * https://developers.google.com/maps/documentation/places/web-service/place-types
 *
 * [primaryOnly] filtra por tipo principal. Hace falta en café y bar: con `includedTypes`
 * Google devuelve McDonald's y estaciones YPF como "cafe" (validado contra la API real).
 * En restaurante no: los tipos principales son específicos (parrilla, pizzería...).
 */
enum class PlaceCategory(val googleTypes: List<String>, val primaryOnly: Boolean) {
    CAFE(listOf("cafe", "coffee_shop"), primaryOnly = true),
    BAR(listOf("bar", "pub", "brewpub", "wine_bar", "cocktail_bar", "bar_and_grill"), primaryOnly = true),
    RESTAURANT(listOf("restaurant"), primaryOnly = false),
    PARK(listOf("park"), primaryOnly = false),
    SHOPPING_MALL(listOf("shopping_mall"), primaryOnly = false),
}

/** Un lugar real candidato a punto de encuentro. */
data class Place(
    val id: String,
    val name: String,
    val location: LatLng,
    val address: String? = null,
    val rating: Double? = null,
)

/** Un lugar sugerido con el tiempo de viaje de cada participante. */
data class Suggestion(
    val place: Place,
    /** Tiempo de viaje por id de participante. */
    val travelTimes: Map<String, Duration>,
    /** Puntaje usado para ordenar: tiempo total ponderado por modo. Menor es mejor. */
    val score: Duration = travelTimes.values.fold(Duration.ZERO) { acc, d -> acc + d },
) {
    val totalTravelTime: Duration = travelTimes.values.fold(Duration.ZERO) { acc, d -> acc + d }
    val maxTravelTime: Duration = travelTimes.values.maxOrNull() ?: Duration.ZERO
}

/** Resultado de una búsqueda de punto de encuentro. */
data class MeetingResult(
    /** Mejores lugares, ordenados por menor tiempo total ponderado por modo. */
    val suggestions: List<Suggestion>,
    /** Zonas donde se buscaron candidatos (las de menor tiempo total muestreado). */
    val searchAreas: List<SearchArea>,
    /** Cuántos candidatos se evaluaron. */
    val candidatesEvaluated: Int,
    /** Candidatos descartados porque alguien no tenía ruta (p. ej., sin transporte público). */
    val discardedUnreachable: Int,
    /** Candidatos descartados por superar el tope de viaje individual. */
    val discardedOverCap: Int,
)

/** Círculo donde se buscaron lugares. */
data class SearchArea(val center: LatLng, val radiusMeters: Double)
