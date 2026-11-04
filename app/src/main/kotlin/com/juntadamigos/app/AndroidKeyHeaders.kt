package com.juntadamigos.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

/**
 * Agrega los headers que Google exige para aceptar una API key restringida a apps Android
 * cuando se llama a sus APIs REST (places.googleapis.com, routes.googleapis.com):
 * paquete y SHA-1 del certificado de firma. Sin esto, una key restringida da 403.
 *
 * Aplica solo a *.googleapis.com. La Geocoding API (maps.googleapis.com) no soporta
 * esta restricción, por eso la app no la usa.
 */
class AndroidKeyHeaders(private val packageName: String, private val certSha1: String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (certSha1 == null || !request.url.host.endsWith("googleapis.com")) return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("X-Android-Package", packageName)
                .header("X-Android-Cert", certSha1)
                .build(),
        )
    }

    companion object {
        fun from(context: Context) = AndroidKeyHeaders(context.packageName, signingCertSha1(context))

        /** SHA-1 del certificado de firma, en hex mayúscula sin separadores (formato que espera Google). */
        @Suppress("DEPRECATION")
        private fun signingCertSha1(context: Context): String? = runCatching {
            val pm = context.packageManager
            val cert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
            } ?: return null
            MessageDigest.getInstance("SHA-1").digest(cert.toByteArray())
                .joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }
}
