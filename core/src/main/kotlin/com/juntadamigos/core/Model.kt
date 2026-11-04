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
 * Tipo de lugar buscado. [googleType] es el tipo de la Places API (New):
 * https://developers.google.com/maps/documentation/places/web-service/place-types
 */
enum class PlaceCategory(val googleType: String) {
    CAFE("cafe"),
    BAR("bar"),
    RESTAURANT("restaurant"),
    PARK("park"),
    SHOPPING_MALL("shopping_mall"),
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
) {
    val totalTravelTime: Duration = travelTimes.values.fold(Duration.ZERO) { acc, d -> acc + d }
    val maxTravelTime: Duration = travelTimes.values.maxOrNull() ?: Duration.ZERO
}

/** Resultado de una búsqueda de punto de encuentro. */
data class MeetingResult(
    /** Mejores lugares, ordenados por menor tiempo total. */
    val suggestions: List<Suggestion>,
    /** Centro geográfico usado para buscar candidatos. */
    val searchCenter: LatLng,
    /** Radio de búsqueda usado, en metros. */
    val searchRadiusMeters: Double,
    /** Cuántos candidatos se evaluaron. */
    val candidatesEvaluated: Int,
    /** Candidatos descartados porque alguien no tenía ruta (p. ej., sin transporte público). */
    val discardedUnreachable: Int,
    /** Candidatos descartados por superar el tope de viaje individual. */
    val discardedOverCap: Int,
)
