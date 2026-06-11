package com.mydrama.sdk

import android.content.Context
import com.mydrama.sdk.internal.DeviceMetadata
import com.mydrama.sdk.internal.InstallationIdStore
import com.mydrama.sdk.internal.MyDramaInterceptor
import com.mydrama.sdk.internal.SessionManager
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Configured MyDrama client and OkHttp integration.
 */
class MyDramaClient private constructor(
    val baseUrl: HttpUrl,
    val okHttpClient: OkHttpClient,
    val interceptor: okhttp3.Interceptor,
    private val sessionManager: SessionManager,
) {
    /**
     * Registers or refreshes the Worker session on the caller's thread.
     * Do not call this method on Android's main thread.
     */
    fun registerSession(): SessionRegistration = sessionManager.registerSession()

    /**
     * Registers or refreshes a session on the SDK executor.
     */
    fun registerSessionAsync(): Future<SessionRegistration> =
        sessionManager.registerSessionAsync()

    /**
     * Registers a session and reports completion on the SDK executor.
     */
    fun registerSessionAsync(callback: SessionCallback): Future<SessionRegistration> =
        sessionManager.registerSessionAsync(callback)

    /**
     * Resolves a Worker-relative path and creates a call with SDK authentication.
     */
    @JvmOverloads
    fun newCall(path: String, requestBuilder: Request.Builder = Request.Builder()): Call {
        val url = baseUrl.resolve(path)
            ?: throw IllegalArgumentException("Path cannot be resolved against the MyDrama base URL: $path")
        return okHttpClient.newCall(requestBuilder.url(url).build())
    }

    /**
     * Adds the MyDrama interceptor to another OkHttp builder.
     */
    fun installOn(builder: OkHttpClient.Builder): OkHttpClient.Builder =
        builder.addInterceptor(interceptor)

    companion object {
        internal fun create(
            context: Context,
            apiKey: String,
            config: MyDramaConfig,
        ): MyDramaClient {
            val metadata = DeviceMetadata.collect(context)
            val installationId = InstallationIdStore(context).getOrCreate()
            val bootstrapClient = OkHttpClient.Builder()
                .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(config.writeTimeoutMillis, TimeUnit.MILLISECONDS)
                .build()
            val sessionManager = SessionManager(
                context = context,
                baseUrl = config.baseUrl,
                apiKey = apiKey,
                installationId = installationId,
                metadata = metadata,
                client = bootstrapClient,
            )
            val interceptor = MyDramaInterceptor(
                baseUrl = config.baseUrl,
                apiKey = apiKey,
                installationId = installationId,
                metadata = metadata,
                sessionManager = sessionManager,
            )
            val client = bootstrapClient.newBuilder()
                .addInterceptor(interceptor)
                .build()
            return MyDramaClient(config.baseUrl, client, interceptor, sessionManager)
        }
    }
}
