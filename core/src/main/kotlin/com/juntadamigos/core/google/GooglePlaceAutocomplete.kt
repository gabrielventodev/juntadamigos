package com.juntadamigos.core.google

import com.juntadamigos.core.AddressAutocomplete
import com.juntadamigos.core.AddressSuggestion
import com.juntadamigos.core.LatLng
import com.juntadamigos.core.ResolvedAddress
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Places API (New): Autocomplete + Place Details.
 * https://developers.google.com/maps/documentation/places/web-service/place-autocomplete
 *
 * Se usa en la app en lugar de Geocoding porque places.googleapis.com acepta keys
 * restringidas a la app Android (headers X-Android-Package / X-Android-Cert);
 * la Geocoding API no.
 */
class GooglePlaceAutocomplete(
    private val apiKey: String,
    private val http: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://places.googleapis.com",
    private val languageCode: String = "es",
    /** Países permitidos (ISO 3166-1 alpha-2, minúsculas), p. ej. listOf("ar"). Vacío = todos. */
    private val regionCodes: List<String> = emptyList(),
    private val biasRadiusMeters: Double = 50_000.0,
) : AddressAutocomplete {

    override suspend fun suggest(input: String, sessionToken: String, bias: LatLng?): List<AddressSuggestion> {
        if (input.isBlank()) return emptyList()
        val request = AutocompleteRequest(
            input = input,
            sessionToken = sessionToken,
            languageCode = languageCode,
            includedRegionCodes = regionCodes.takeIf { it.isNotEmpty() },
            locationBias = bias?.let {
                Bias(Circle(LatLngJson(it.latitude, it.longitude), biasRadiusMeters.coerceIn(1.0, 50_000.0)))
            },
        )
        val text = http.postGoogle(
            url = "$baseUrl/v1/places:autocomplete",
            apiKey = apiKey,
            fieldMask = AUTOCOMPLETE_FIELD_MASK,
            body = googleJson.encodeToString(request),
        )
        return parseAutocompleteResponse(text)
    }

    override suspend fun resolve(placeId: String, sessionToken: String): ResolvedAddress? {
        val url = "$baseUrl/v1/places".toHttpUrl().newBuilder()
            .addPathSegment(placeId)
            .addQueryParameter("sessionToken", sessionToken)
            .addQueryParameter("languageCode", languageCode)
            .build()
            .toString()
        return parseDetailsResponse(http.getGoogle(url, apiKey, DETAILS_FIELD_MASK))
    }

    companion object {
        const val AUTOCOMPLETE_FIELD_MASK =
            "suggestions.placePrediction.placeId,suggestions.placePrediction.text,suggestions.placePrediction.structuredFormat"

        // Solo campos del SKU "Essentials" de Place Details.
        const val DETAILS_FIELD_MASK = "id,formattedAddress,location"

        internal fun parseAutocompleteResponse(text: String): List<AddressSuggestion> =
            googleJson.decodeFromString<AutocompleteResponse>(text).suggestions.mapNotNull { s ->
                val p = s.placePrediction ?: return@mapNotNull null // queryPrediction no tiene placeId
                val main = p.structuredFormat?.mainText?.text ?: p.text?.text ?: return@mapNotNull null
                AddressSuggestion(p.placeId, main, p.structuredFormat?.secondaryText?.text)
            }

        internal fun parseDetailsResponse(text: String): ResolvedAddress? {
            val d = googleJson.decodeFromString<DetailsResponse>(text)
            val loc = d.location ?: return null
            return ResolvedAddress(d.id, d.formattedAddress ?: d.id, LatLng(loc.latitude, loc.longitude))
        }
    }

    @Serializable
    internal data class AutocompleteRequest(
        val input: String,
        val sessionToken: String,
        val languageCode: String,
        val includedRegionCodes: List<String>? = null,
        val locationBias: Bias? = null,
    )

    @Serializable
    internal data class Bias(val circle: Circle)

    @Serializable
    internal data class Circle(val center: LatLngJson, val radius: Double)

    @Serializable
    internal data class LatLngJson(val latitude: Double, val longitude: Double)

    @Serializable
    internal data class AutocompleteResponse(val suggestions: List<SuggestionJson> = emptyList())

    @Serializable
    internal data class SuggestionJson(val placePrediction: PlacePrediction? = null)

    @Serializable
    internal data class PlacePrediction(
        val placeId: String,
        val text: FormattableText? = null,
        val structuredFormat: StructuredFormat? = null,
    )

    @Serializable
    internal data class StructuredFormat(
        val mainText: FormattableText? = null,
        val secondaryText: FormattableText? = null,
    )

    @Serializable
    internal data class FormattableText(val text: String)

    @Serializable
    internal data class DetailsResponse(
        val id: String,
        val formattedAddress: String? = null,
        val location: LatLngJson? = null,
    )
}
