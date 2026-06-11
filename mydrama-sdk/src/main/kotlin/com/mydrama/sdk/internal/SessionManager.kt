package com.mydrama.sdk.internal

import android.content.Context
import android.os.SystemClock
import com.mydrama.sdk.SessionCallback
import com.mydrama.sdk.SessionRegistration
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal class SessionManager(
    context: Context,
    private val baseUrl: HttpUrl,
    private val apiKey: String,
    private val installationId: String,
    private val metadata: DeviceMetadata,
    private val client: OkHttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mydrama-session").apply { isDaemon = true }
    },
) {
    private val sessionStore = SessionStore(
        context,
        sha256("${metadata.packageName}|$baseUrl|$apiKey").take(24),
    )
    private val registrationLock = Any()

    fun validToken(): String? = sessionStore.read(clock())?.token

    fun registerSessionAsync(): Future<SessionRegistration> =
        executor.submit<SessionRegistration> { registerSession() }

    fun registerSessionAsync(callback: SessionCallback): Future<SessionRegistration> =
        executor.submit<SessionRegistration> {
            try {
                registerSession().also { callback.onComplete(it, null) }
            } catch (error: Throwable) {
                callback.onComplete(null, error)
                throw error
            }
        }

    fun registerSession(): SessionRegistration = synchronized(registrationLock) {
        sessionStore.read(clock())?.let {
            return@synchronized SessionRegistration(
                registered = true,
                sessionToken = it.token,
                expiresAtEpochMillis = it.expiresAtEpochMillis,
            )
        }

        val elapsed = elapsedRealtime()
        val requestBuilder = Request.Builder()
            .url(baseUrl.resolve("api/session/register")!!)
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("X-App-Secure-Token", apiKey)
            .header("X-App-Instance-ID", installationId)

        for ((name, value) in metadata.headers(elapsed)) {
            requestBuilder.header(name, value)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("MyDrama session registration failed with HTTP ${response.code}.")
            }

            val json = try {
                JSONObject(body)
            } catch (error: Exception) {
                throw IOException("MyDrama session registration returned invalid JSON.", error)
            }
            val token = json.optString("sessionToken").takeIf(String::isNotBlank)
                ?: throw IOException("MyDrama session registration did not return a session token.")
            val expiresAt = parseExpiresAt(json, clock())
            val registration = SessionRegistration(
                registered = json.optBoolean("registered", false),
                sessionToken = token,
                expiresAtEpochMillis = expiresAt,
                mode = json.optNullableString("mode"),
                provider = json.optNullableString("provider"),
                gatePassed = json.optNullableBoolean("gatePassed"),
                riskLevel = json.optNullableString("riskLevel"),
            )
            sessionStore.write(StoredSession(token, expiresAt))
            registration
        }
    }

    private fun parseExpiresAt(json: JSONObject, now: Long): Long {
        val expiresInSeconds = json.optLong("expiresIn", 0L)
        if (expiresInSeconds > 0) return now + expiresInSeconds * 1_000L

        val expiresAt = json.optString("expiresAt")
        if (expiresAt.isNotBlank()) {
            parseIsoTimestamp(expiresAt)?.let { return it }
        }
        throw IOException("MyDrama session registration did not return a valid expiry.")
    }

    private fun parseIsoTimestamp(value: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(value)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf(String::isNotBlank) else null

private fun JSONObject.optNullableBoolean(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null
