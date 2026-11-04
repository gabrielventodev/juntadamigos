package com.juntadamigos.core.google

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GoogleApiException(val httpCode: Int, message: String) : RuntimeException(message)

internal val googleJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/** POST JSON a una API de Google Maps Platform (Places New, Routes) y devuelve el cuerpo. */
internal suspend fun OkHttpClient.postGoogle(
    url: String,
    apiKey: String,
    fieldMask: String,
    body: String,
): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .header("X-Goog-Api-Key", apiKey)
        .header("X-Goog-FieldMask", fieldMask)
        .post(body.toRequestBody(JSON_MEDIA))
        .build()
    newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw GoogleApiException(response.code, "HTTP ${response.code}: $text")
        text
    }
}

internal suspend fun OkHttpClient.getText(url: String): String = withContext(Dispatchers.IO) {
    newCall(Request.Builder().url(url).get().build()).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw GoogleApiException(response.code, "HTTP ${response.code}: $text")
        text
    }
}
