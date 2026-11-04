package com.juntadamigos.core.google

import com.juntadamigos.core.Geocoder
import com.juntadamigos.core.LatLng
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Geocoding API: dirección escrita a coordenadas.
 * En la app, el autocompletado de Places reemplaza esto para la carga de amigos;
 * queda como alternativa para direcciones pegadas a mano.
 */
class GoogleGeocoder(
    private val apiKey: String,
    private val http: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://maps.googleapis.com",
    private val language: String = "es",
    /** Sesgo por país (ISO 3166-1), p. ej. "ar". */
    private val region: String? = null,
) : Geocoder {

    override suspend fun geocode(address: String): LatLng? {
        val url = "$baseUrl/maps/api/geocode/json".toHttpUrl().newBuilder()
            .addQueryParameter("address", address)
            .addQueryParameter("language", language)
            .apply { region?.let { addQueryParameter("region", it) } }
            .addQueryParameter("key", apiKey)
            .build()
            .toString()
        return parseGeocodeResponse(http.getText(url))
    }

    companion object {
        internal fun parseGeocodeResponse(text: String): LatLng? {
            val response = googleJson.decodeFromString<GeocodeResponse>(text)
            return when (response.status) {
                "OK" -> response.results.firstOrNull()?.geometry?.location?.let { LatLng(it.lat, it.lng) }
                "ZERO_RESULTS" -> null
                else -> throw GoogleApiException(200, "Geocoding ${response.status}: ${response.errorMessage.orEmpty()}")
            }
        }
    }

    @Serializable
    internal data class GeocodeResponse(
        val status: String,
        val results: List<Result> = emptyList(),
        @kotlinx.serialization.SerialName("error_message") val errorMessage: String? = null,
    )

    @Serializable
    internal data class Result(val geometry: Geometry? = null)

    @Serializable
    internal data class Geometry(val location: Loc? = null)

    @Serializable
    internal data class Loc(val lat: Double, val lng: Double)
}
