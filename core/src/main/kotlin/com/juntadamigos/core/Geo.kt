package com.juntadamigos.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Distancia sobre la esfera (fórmula de haversine), en metros. */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Centro geográfico (promedio de vectores 3D). Correcto cerca del antimeridiano,
     * a diferencia de promediar latitudes y longitudes.
     */
    fun centroid(points: List<LatLng>): LatLng {
        require(points.isNotEmpty()) { "Se necesita al menos un punto" }
        var x = 0.0
        var y = 0.0
        var z = 0.0
        for (p in points) {
            val lat = Math.toRadians(p.latitude)
            val lon = Math.toRadians(p.longitude)
            x += cos(lat) * cos(lon)
            y += cos(lat) * sin(lon)
            z += sin(lat)
        }
        val n = points.size
        x /= n; y /= n; z /= n
        val lon = atan2(y, x)
        val lat = atan2(z, sqrt(x * x + y * y))
        return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
    }
}
