package com.juntadamigos.core

import kotlin.time.Duration

/** Busca lugares reales de una categoría alrededor de un punto. */
interface PlaceSearch {
    suspend fun searchNearby(
        center: LatLng,
        radiusMeters: Double,
        category: PlaceCategory,
        maxResults: Int,
    ): List<Place>
}

/**
 * Calcula tiempos de viaje de varios orígenes a varios destinos con un mismo modo.
 * Devuelve una matriz [origen][destino]; null significa que no hay ruta.
 */
interface TravelTimeMatrix {
    suspend fun travelTimes(
        origins: List<LatLng>,
        destinations: List<LatLng>,
        mode: TravelMode,
    ): List<List<Duration?>>
}

/** Convierte una dirección escrita en coordenadas. */
interface Geocoder {
    suspend fun geocode(address: String): LatLng?
}
