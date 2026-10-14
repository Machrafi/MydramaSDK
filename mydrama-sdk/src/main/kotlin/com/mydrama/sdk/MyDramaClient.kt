package com.mydrama.sdk

import android.content.Context
import com.mydrama.sdk.internal.CredentialRedirectGuard
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
     * Registers or refreshes the DramaSDK API session on the caller's thread.
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
     * Resolves a DramaSDK API-relative path and creates a call with SDK authentication.
     */
    @JvmOverloads
    fun newCall(path: String, requestBuilder: Request.Builder = Request.Builder()): Call {
        val url = baseUrl.resolve(path)
            ?: throw IllegalArgumentException("Path cannot be resolved against the MyDrama base URL: $path")
        require(url.scheme == baseUrl.scheme && url.host == baseUrl.host && url.port == baseUrl.port) {
            "SDK calls must stay on the configured API origin. Use a separate client for external URLs."
        }
        return okHttpClient.newCall(requestBuilder.url(url).build())
    }

    @JvmOverloads
    fun home(lang: String = "en", pageNo: Int = 1, pageSize: Int = 20): Call {
        require(pageNo in 1..10000 && pageSize in 1..500) { "Invalid catalog pagination." }
        val url = baseUrl.newBuilder().addPathSegments("api/home")
            .addQueryParameter("lang", lang).addQueryParameter("pageNo", pageNo.toString())
            .addQueryParameter("pageSize", pageSize.toString()).build()
        return newCall(url.toString())
    }

    @JvmOverloads
    fun detail(storyId: String, lang: String = "en"): Call =
        storyCall("detail", storyId, lang, true)

    @JvmOverloads
    fun chapters(storyId: String, index: Int = 1, episodePageSize: Int = 100, lang: String = "en"): Call {
        require(index > 0 && episodePageSize in 1..500) { "Invalid chapter pagination." }
        require(storyId.isNotBlank()) { "A story ID is required." }
        val url = baseUrl.newBuilder().addPathSegments("api/chapters").addPathSegment(storyId)
            .addQueryParameter("index", index.toString()).addQueryParameter("episodePageSize", episodePageSize.toString())
            .addQueryParameter("lang", lang).build()
        return newCall(url.toString())
    }

    @JvmOverloads
    fun stream(storyId: String, episode: Int = 1, lang: String = "en"): Call {
        require(storyId.isNotBlank() && episode > 0) { "A story ID and positive episode are required." }
        val url = baseUrl.newBuilder().addPathSegments("api/stream").addQueryParameter("bookId", storyId)
            .addQueryParameter("episode", episode.toString()).addQueryParameter("lang", lang).build()
        return newCall(url.toString())
    }

    private fun storyCall(endpoint: String, storyId: String, lang: String, versioned: Boolean): Call {
        require(storyId.isNotBlank()) { "A story ID is required." }
        val builder = baseUrl.newBuilder().addPathSegments("api/$endpoint").addPathSegment(storyId)
        if (versioned) builder.addPathSegment("v2")
        return newCall(builder.addQueryParameter("lang", lang).build().toString())
    }

    /**
     * Adds the MyDrama interceptor to another OkHttp builder.
     */
    fun installOn(builder: OkHttpClient.Builder): OkHttpClient.Builder =
        builder.addInterceptor(interceptor).addNetworkInterceptor(CredentialRedirectGuard(baseUrl))

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
                .followRedirects(false)
                .followSslRedirects(false)
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
                .addNetworkInterceptor(CredentialRedirectGuard(config.baseUrl))
                .build()
            return MyDramaClient(config.baseUrl, client, interceptor, sessionManager)
        }
    }
}
