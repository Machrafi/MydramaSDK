package com.mydrama.sdk

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

/**
 * Runtime options for the MyDrama SDK.
 *
 * Use [Builder] from Java or [build] from Kotlin.
 */
class MyDramaConfig private constructor(
    val baseUrl: HttpUrl,
    val connectTimeoutMillis: Long,
    val readTimeoutMillis: Long,
    val writeTimeoutMillis: Long,
    val registerSessionOnInitialize: Boolean,
) {
    class Builder {
        private var baseUrl: HttpUrl? = null
        private var connectTimeoutMillis: Long = TimeUnit.SECONDS.toMillis(15)
        private var readTimeoutMillis: Long = TimeUnit.SECONDS.toMillis(30)
        private var writeTimeoutMillis: Long = TimeUnit.SECONDS.toMillis(30)
        private var registerSessionOnInitialize: Boolean = true

        fun baseUrl(baseUrl: String) = apply {
            val normalized = baseUrl.trim().let { if (it.endsWith('/')) it else "$it/" }
            this.baseUrl = normalized.toHttpUrl()
            require(this.baseUrl!!.isHttps || isLocalhost(this.baseUrl!!.host)) {
                "MyDrama base URL must use HTTPS (HTTP is allowed only for localhost)."
            }
        }

        fun connectTimeout(timeout: Long, unit: TimeUnit) = apply {
            connectTimeoutMillis = positiveMillis(timeout, unit)
        }

        fun readTimeout(timeout: Long, unit: TimeUnit) = apply {
            readTimeoutMillis = positiveMillis(timeout, unit)
        }

        fun writeTimeout(timeout: Long, unit: TimeUnit) = apply {
            writeTimeoutMillis = positiveMillis(timeout, unit)
        }

        fun registerSessionOnInitialize(enabled: Boolean) = apply {
            registerSessionOnInitialize = enabled
        }

        fun build(): MyDramaConfig {
            val url = baseUrl
                ?: throw IllegalStateException("MyDrama base URL is required. Call baseUrl() in the builder.")
            return MyDramaConfig(
                baseUrl = url,
                connectTimeoutMillis = connectTimeoutMillis,
                readTimeoutMillis = readTimeoutMillis,
                writeTimeoutMillis = writeTimeoutMillis,
                registerSessionOnInitialize = registerSessionOnInitialize,
            )
        }

        private fun positiveMillis(timeout: Long, unit: TimeUnit): Long {
            require(timeout > 0) { "Timeout must be greater than zero." }
            return unit.toMillis(timeout).also {
                require(it > 0) { "Timeout is too small for millisecond precision." }
            }
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        inline fun build(block: Builder.() -> Unit): MyDramaConfig =
            Builder().apply(block).build()

        private fun isLocalhost(host: String): Boolean =
            host == "localhost" || host == "127.0.0.1" || host == "::1"
    }
}
