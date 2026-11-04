package com.juntadamigos.app

import android.content.Context
import com.juntadamigos.core.AddressAutocomplete
import com.juntadamigos.core.MeetingPointFinder
import com.juntadamigos.core.google.GooglePlaceAutocomplete
import com.juntadamigos.core.google.GooglePlaceSearch
import com.juntadamigos.core.google.GoogleRouteMatrix
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Dependencias de la app. Sin framework de DI: son 3 objetos. */
class AppGraph(context: Context) {
    private val apiKey: String = BuildConfig.MAPS_API_KEY

    val hasApiKey: Boolean = apiKey.isNotBlank() && apiKey != "DEFAULT_API_KEY"

    private val http = OkHttpClient.Builder()
        .addInterceptor(AndroidKeyHeaders.from(context))
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    val autocomplete: AddressAutocomplete = GooglePlaceAutocomplete(apiKey, http)

    val finder = MeetingPointFinder(
        placeSearch = GooglePlaceSearch(apiKey, http),
        travelTimeMatrix = GoogleRouteMatrix(apiKey, http),
    )
}
