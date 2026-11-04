package com.juntadamigos.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class GeoTest {
    private fun near(expected: Double, actual: Double, tolerance: Double) =
        assertTrue(abs(expected - actual) <= tolerance, "esperado $expected, obtenido $actual")

    @Test
    fun `distancia Obelisco a Plaza de Mayo`() {
        val obelisco = LatLng(-34.6037, -58.3816)
        val plazaDeMayo = LatLng(-34.6083, -58.3712)
        near(1_080.0, Geo.distanceMeters(obelisco, plazaDeMayo), 30.0)
    }

    @Test
    fun `centro de dos puntos simetricos`() {
        val c = Geo.centroid(listOf(LatLng(-34.0, -58.0), LatLng(-34.0, -59.0)))
        near(-58.5, c.longitude, 1e-6)
        near(-34.0, c.latitude, 0.01)
    }

    @Test
    fun `centro funciona cruzando el antimeridiano`() {
        val c = Geo.centroid(listOf(LatLng(0.0, 179.0), LatLng(0.0, -179.0)))
        near(180.0, abs(c.longitude), 1e-6)
    }
}
