package com.juntadamigos.core.google

import com.juntadamigos.core.LatLng
import com.juntadamigos.core.Place
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.PlaceSearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient

/**
 * Places API (New), Nearby Search.
 * https://developers.google.com/maps/documentation/places/web-service/nearby-search
 */
class GooglePlaceSearch(
    private val apiKey: String,
    private val http: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://places.googleapis.com",
    private val languageCode: String = "es",
) : PlaceSearch {

    override suspend fun searchNearby(
        center: LatLng,
        radiusMeters: Double,
        category: PlaceCategory,
        maxResults: Int,
    ): List<Place> {
        val request = NearbyRequest(
            includedTypes = listOf(category.googleType),
            maxResultCount = maxResults.coerceIn(1, 20),
            locationRestriction = LocationRestriction(
                Circle(LatLngJson(center.latitude, center.longitude), radiusMeters.coerceIn(1.0, 50_000.0)),
            ),
            languageCode = languageCode,
        )
        val text = http.postGoogle(
            url = "$baseUrl/v1/places:searchNearby",
            apiKey = apiKey,
            fieldMask = FIELD_MASK,
            body = googleJson.encodeToString(request),
        )
        return parseNearbyResponse(text)
    }

    companion object {
        // Solo pedimos campos de los SKU más baratos que necesitamos; rating sube el SKU a "Pro".
        const val FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,places.rating"

        internal fun parseNearbyResponse(text: String): List<Place> =
            googleJson.decodeFromString<NearbyResponse>(text).places.mapNotNull { p ->
                val loc = p.location ?: return@mapNotNull null
                Place(
                    id = p.id,
                    name = p.displayName?.text ?: p.id,
                    location = LatLng(loc.latitude, loc.longitude),
                    address = p.formattedAddress,
                    rating = p.rating,
                )
            }
    }

    @Serializable
    internal data class NearbyRequest(
        val includedTypes: List<String>,
        val maxResultCount: Int,
        val locationRestriction: LocationRestriction,
        val languageCode: String,
        val rankPreference: String = "POPULARITY",
    )

    @Serializable
    internal data class LocationRestriction(val circle: Circle)

    @Serializable
    internal data class Circle(val center: LatLngJson, val radius: Double)

    @Serializable
    internal data class LatLngJson(val latitude: Double, val longitude: Double)

    @Serializable
    internal data class NearbyResponse(val places: List<PlaceJson> = emptyList())

    @Serializable
    internal data class PlaceJson(
        val id: String,
        val displayName: LocalizedText? = null,
        val formattedAddress: String? = null,
        val location: LatLngJson? = null,
        val rating: Double? = null,
    )

    @Serializable
    internal data class LocalizedText(val text: String)
}
