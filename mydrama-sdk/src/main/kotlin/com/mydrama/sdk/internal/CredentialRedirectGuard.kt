package com.mydrama.sdk.internal

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

internal class SdkAuthenticatedRequest

/** Protects SDK headers even when installed into a client that follows redirects. */
internal class CredentialRedirectGuard(private val origin: HttpUrl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.tag(SdkAuthenticatedRequest::class.java) != null &&
            (request.url.scheme != origin.scheme || request.url.host != origin.host || request.url.port != origin.port)) {
            val clean = request.newBuilder().removeHeader("Authorization")
            for (name in request.headers.names()) {
                if (name.startsWith("X-App-", ignoreCase = true) || name.startsWith("X-Device-", ignoreCase = true) ||
                    name.startsWith("X-MyDrama-", ignoreCase = true) || name == "X-Android-API-Level") clean.removeHeader(name)
            }
            clean.header("User-Agent", "DramaSDK-Android")
            return chain.proceed(clean.build())
        }
        return chain.proceed(request)
    }
}
