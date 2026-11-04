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

/** Una sugerencia de autocompletado de dirección o lugar. */
data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String? = null,
)

/** Una dirección elegida, ya con coordenadas. */
data class ResolvedAddress(
    val placeId: String,
    val address: String,
    val location: LatLng,
)

/**
 * Autocompletado de direcciones en dos pasos: [suggest] mientras se escribe y [resolve]
 * al elegir. Ambos usan el mismo [sessionToken] para que Google cobre una sola sesión.
 * Después de [resolve], generar un token nuevo.
 */
interface AddressAutocomplete {
    suspend fun suggest(input: String, sessionToken: String, bias: LatLng? = null): List<AddressSuggestion>
    suspend fun resolve(placeId: String, sessionToken: String): ResolvedAddress?
}
