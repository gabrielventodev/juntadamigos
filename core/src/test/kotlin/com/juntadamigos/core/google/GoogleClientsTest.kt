package com.juntadamigos.core.google

import com.juntadamigos.core.LatLng
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.TravelMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class GoogleClientsTest {
    private lateinit var server: MockWebServer
    private val baseUrl get() = server.url("/").toString().trimEnd('/')

    @BeforeTest fun start() { server = MockWebServer().apply { start() } }
    @AfterTest fun stop() { server.shutdown() }

    @Test
    fun `places envia key, field mask y tipo, y parsea la respuesta`() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"places":[
                {"id":"p1","displayName":{"text":"Café Tortoni","languageCode":"es"},
                 "formattedAddress":"Av. de Mayo 825","location":{"latitude":-34.6087,"longitude":-58.3786},"rating":4.5},
                {"id":"sin-ubicacion","displayName":{"text":"X"}}
            ]}""",
        ))
        val places = GooglePlaceSearch("KEY", baseUrl = baseUrl)
            .searchNearby(LatLng(-34.6, -58.38), 2_000.0, PlaceCategory.CAFE, 15)

        assertEquals(1, places.size)
        assertEquals("Café Tortoni", places[0].name)
        assertEquals(4.5, places[0].rating)

        val req = server.takeRequest()
        assertEquals("/v1/places:searchNearby", req.path)
        assertEquals("KEY", req.getHeader("X-Goog-Api-Key"))
        assertEquals(GooglePlaceSearch.FIELD_MASK, req.getHeader("X-Goog-FieldMask"))
        val body = Json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals("cafe", body["includedTypes"]!!.jsonArray[0].jsonPrimitive.content)
        assertEquals(15, body["maxResultCount"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `places sin resultados devuelve lista vacia`() {
        assertEquals(emptyList(), GooglePlaceSearch.parseNearbyResponse("{}"))
    }

    @Test
    fun `matriz parsea duraciones, omite rutas inexistentes y respeta indices omitidos`() = runTest {
        server.enqueue(MockResponse().setBody(
            """[
                {"destinationIndex":1,"duration":"600s","condition":"ROUTE_EXISTS"},
                {"originIndex":1,"duration":"120s","condition":"ROUTE_EXISTS"},
                {"condition":"ROUTE_EXISTS","duration":"30s"},
                {"originIndex":1,"destinationIndex":1,"condition":"ROUTE_NOT_FOUND"}
            ]""",
        ))
        val o = listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.1))
        val d = listOf(LatLng(0.0, 0.05), LatLng(0.0, 0.06))
        val m = GoogleRouteMatrix("KEY", baseUrl = baseUrl).travelTimes(o, d, TravelMode.TRANSIT)

        assertEquals(30.seconds, m[0][0])
        assertEquals(600.seconds, m[0][1])
        assertEquals(120.seconds, m[1][0])
        assertNull(m[1][1])

        val req = server.takeRequest()
        assertEquals("/distanceMatrix/v2:computeRouteMatrix", req.path)
        val body = Json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals("TRANSIT", body["travelMode"]!!.jsonPrimitive.content)
        assertNull(body["routingPreference"], "TRANSIT no acepta routingPreference")
    }

    @Test
    fun `matriz con auto pide trafico en tiempo real`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        GoogleRouteMatrix("KEY", baseUrl = baseUrl).travelTimes(listOf(LatLng(0.0, 0.0)), listOf(LatLng(0.0, 0.1)), TravelMode.DRIVE)
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("TRAFFIC_AWARE", body["routingPreference"]!!.jsonPrimitive.content)
    }

    @Test
    fun `error HTTP se propaga con el codigo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":{"message":"API key not valid"}}"""))
        val e = assertFailsWith<GoogleApiException> {
            GoogleRouteMatrix("MALA", baseUrl = baseUrl).travelTimes(listOf(LatLng(0.0, 0.0)), listOf(LatLng(0.0, 0.1)), TravelMode.DRIVE)
        }
        assertEquals(403, e.httpCode)
    }

    @Test
    fun `geocoding devuelve coordenadas, null sin resultados y error si falla`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"OK","results":[{"geometry":{"location":{"lat":-34.6,"lng":-58.4}}}]}"""))
        server.enqueue(MockResponse().setBody("""{"status":"ZERO_RESULTS","results":[]}"""))
        server.enqueue(MockResponse().setBody("""{"status":"REQUEST_DENIED","error_message":"key","results":[]}"""))
        val g = GoogleGeocoder("KEY", baseUrl = baseUrl, region = "ar")

        assertEquals(LatLng(-34.6, -58.4), g.geocode("Av. Corrientes 1234, CABA"))
        assertNull(g.geocode("asdfqwer"))
        assertFailsWith<GoogleApiException> { g.geocode("x") }

        val first = server.takeRequest()
        assertEquals("ar", first.requestUrl!!.queryParameter("region"))
        assertEquals("Av. Corrientes 1234, CABA", first.requestUrl!!.queryParameter("address"))
    }

    @Test
    fun `duraciones con decimales`() {
        assertEquals(90.5.seconds, GoogleRouteMatrix.parseGoogleDuration("90.5s"))
        assertNull(GoogleRouteMatrix.parseGoogleDuration("abc"))
    }
}
