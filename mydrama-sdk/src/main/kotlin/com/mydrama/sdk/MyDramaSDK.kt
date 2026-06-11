package com.mydrama.sdk

import android.content.Context

/**
 * Process-wide entry point for the MyDrama Android SDK.
 */
object MyDramaSDK {
    @Volatile
    private var currentClient: MyDramaClient? = null

    /**
     * Initializes the SDK. A base URL must be provided via [MyDramaConfig].
     */
    @JvmStatic
    fun initialize(
        context: Context,
        apiKey: String,
        config: MyDramaConfig,
    ): MyDramaClient {
        validateApiKey(apiKey)
        return MyDramaClient.create(context.applicationContext, apiKey, config).also {
            currentClient = it
            if (config.registerSessionOnInitialize) {
                it.registerSessionAsync()
            }
        }
    }

    /**
     * Returns the most recently initialized client.
     */
    @JvmStatic
    fun getClient(): MyDramaClient =
        currentClient ?: throw IllegalStateException(
            "MyDramaSDK is not initialized. Call MyDramaSDK.initialize(context, apiKey) first.",
        )

    internal fun resetForTests() {
        currentClient = null
    }

    private fun validateApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) { "MyDrama API key must not be blank." }
        require(apiKey.none(Char::isWhitespace)) { "MyDrama API key must not contain whitespace." }
    }
}
