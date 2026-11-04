package com.juntadamigos.core.google

import com.juntadamigos.core.LatLng
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
import kotlin.test.assertNull

class GooglePlaceAutocompleteTest {
    private lateinit var server: MockWebServer
    private val baseUrl get() = server.url("/").toString().trimEnd('/')

    @BeforeTest fun start() { server = MockWebServer().apply { start() } }
    @AfterTest fun stop() { server.shutdown() }

    @Test
    fun `autocompletado envia token, sesgo y region, e ignora predicciones de consulta`() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"suggestions":[
                {"placePrediction":{"placeId":"abc","text":{"text":"Av. Corrientes 1500, CABA, Argentina"},
                  "structuredFormat":{"mainText":{"text":"Av. Corrientes 1500"},"secondaryText":{"text":"CABA, Argentina"}}}},
                {"queryPrediction":{"text":{"text":"corrientes pizza"}}}
            ]}""",
        ))
        val ac = GooglePlaceAutocomplete("KEY", baseUrl = baseUrl, regionCodes = listOf("ar"))
        val s = ac.suggest("corrientes 15", "tok-1", bias = LatLng(-34.6, -58.4))

        assertEquals(1, s.size)
        assertEquals("abc", s[0].placeId)
        assertEquals("Av. Corrientes 1500", s[0].primaryText)
        assertEquals("CABA, Argentina", s[0].secondaryText)

        val req = server.takeRequest()
        assertEquals("/v1/places:autocomplete", req.path)
        assertEquals(GooglePlaceAutocomplete.AUTOCOMPLETE_FIELD_MASK, req.getHeader("X-Goog-FieldMask"))
        val body = Json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals("tok-1", body["sessionToken"]!!.jsonPrimitive.content)
        assertEquals("ar", body["includedRegionCodes"]!!.jsonArray[0].jsonPrimitive.content)
        assertEquals("-34.6", body["locationBias"]!!.jsonObject["circle"]!!.jsonObject["center"]!!
            .jsonObject["latitude"]!!.jsonPrimitive.content)
    }

    @Test
    fun `sin region ni sesgo no se mandan esos campos`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        val s = GooglePlaceAutocomplete("KEY", baseUrl = baseUrl).suggest("x", "t")
        assertEquals(emptyList(), s)
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertNull(body["includedRegionCodes"])
        assertNull(body["locationBias"])
    }

    @Test
    fun `detalles devuelve coordenadas con el mismo token`() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"id":"abc","formattedAddress":"Av. Corrientes 1500, C1042 CABA","location":{"latitude":-34.604,"longitude":-58.387}}""",
        ))
        val r = GooglePlaceAutocomplete("KEY", baseUrl = baseUrl).resolve("abc", "tok-1")!!
        assertEquals(LatLng(-34.604, -58.387), r.location)
        assertEquals("Av. Corrientes 1500, C1042 CABA", r.address)

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/v1/places/abc", req.requestUrl!!.encodedPath)
        assertEquals("tok-1", req.requestUrl!!.queryParameter("sessionToken"))
        assertEquals("KEY", req.getHeader("X-Goog-Api-Key"))
        assertEquals(GooglePlaceAutocomplete.DETAILS_FIELD_MASK, req.getHeader("X-Goog-FieldMask"))
    }

    @Test
    fun `detalles sin ubicacion devuelve null`() {
        assertNull(GooglePlaceAutocomplete.parseDetailsResponse("""{"id":"abc"}"""))
    }
}
