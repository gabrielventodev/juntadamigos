package com.juntadamigos.core.google

import com.juntadamigos.core.LatLng
import com.juntadamigos.core.TravelMode
import com.juntadamigos.core.TravelTimeMatrix
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Routes API, computeRouteMatrix.
 * https://developers.google.com/maps/documentation/routes/compute_route_matrix
 *
 * Límites: 625 elementos por llamada, 100 con TRANSIT. [MeetingPointFinder] ya parte en lotes.
 */
class GoogleRouteMatrix(
    private val apiKey: String,
    private val http: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://routes.googleapis.com",
    /** Tráfico en tiempo real para auto. Cuesta más (SKU Advanced); desactivable. */
    private val trafficAware: Boolean = true,
) : TravelTimeMatrix {

    override suspend fun travelTimes(
        origins: List<LatLng>,
        destinations: List<LatLng>,
        mode: TravelMode,
    ): List<List<Duration?>> {
        if (origins.isEmpty() || destinations.isEmpty()) return origins.map { emptyList() }
        val request = MatrixRequest(
            origins = origins.map { Waypoint(it) },
            destinations = destinations.map { Waypoint(it) },
            travelMode = mode.name,
            routingPreference = if (mode == TravelMode.DRIVE) {
                if (trafficAware) "TRAFFIC_AWARE" else "TRAFFIC_UNAWARE"
            } else {
                null // Solo DRIVE y TWO_WHEELER aceptan routingPreference.
            },
        )
        val text = http.postGoogle(
            url = "$baseUrl/distanceMatrix/v2:computeRouteMatrix",
            apiKey = apiKey,
            fieldMask = FIELD_MASK,
            body = googleJson.encodeToString(request),
        )
        return parseMatrixResponse(text, origins.size, destinations.size)
    }

    companion object {
        const val FIELD_MASK = "originIndex,destinationIndex,duration,condition"

        internal fun parseMatrixResponse(text: String, rows: Int, cols: Int): List<List<Duration?>> {
            val result = List(rows) { arrayOfNulls<Duration>(cols) }
            for (e in googleJson.decodeFromString<List<MatrixElement>>(text)) {
                if (e.condition != "ROUTE_EXISTS") continue
                val d = e.duration?.let(::parseGoogleDuration) ?: continue
                if (e.originIndex in 0 until rows && e.destinationIndex in 0 until cols) {
                    result[e.originIndex][e.destinationIndex] = d
                }
            }
            return result.map { it.toList() }
        }

        /** Google serializa duraciones como "123s" o "123.5s". */
        internal fun parseGoogleDuration(raw: String): Duration? =
            raw.removeSuffix("s").toDoubleOrNull()?.seconds
    }

    @Serializable
    internal data class MatrixRequest(
        val origins: List<Waypoint>,
        val destinations: List<Waypoint>,
        val travelMode: String,
        val routingPreference: String? = null,
    )

    @Serializable
    internal data class Waypoint(val waypoint: WaypointLocation) {
        constructor(latLng: LatLng) : this(
            WaypointLocation(LocationJson(LatLngJson(latLng.latitude, latLng.longitude))),
        )
    }

    @Serializable
    internal data class WaypointLocation(val location: LocationJson)

    @Serializable
    internal data class LocationJson(val latLng: LatLngJson)

    @Serializable
    internal data class LatLngJson(val latitude: Double, val longitude: Double)

    @Serializable
    internal data class MatrixElement(
        // proto3 omite los campos en 0, por eso el default.
        val originIndex: Int = 0,
        val destinationIndex: Int = 0,
        val duration: String? = null,
        val condition: String? = null,
    )
}
