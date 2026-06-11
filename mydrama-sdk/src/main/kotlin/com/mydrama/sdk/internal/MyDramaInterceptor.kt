package com.mydrama.sdk.internal

import android.os.SystemClock
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

internal class MyDramaInterceptor(
    private val baseUrl: HttpUrl,
    private val apiKey: String,
    private val installationId: String,
    private val metadata: DeviceMetadata,
    private val sessionManager: SessionManager,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!sameOrigin(original.url, baseUrl)) {
            return chain.proceed(original)
        }

        val builder = original.newBuilder()
            .header("X-App-Secure-Token", apiKey)
            .header("X-App-Instance-ID", installationId)
        for ((name, value) in metadata.headers(elapsedRealtime())) {
            builder.header(name, value)
        }
        sessionManager.validToken()?.let { builder.header("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }

    private fun sameOrigin(left: HttpUrl, right: HttpUrl): Boolean =
        left.scheme == right.scheme && left.host == right.host && left.port == right.port
}
