package com.mydrama.sdk.internal

import android.content.Context

internal class SessionStore(context: Context, storageKey: String) {
    private val preferences = context.getSharedPreferences(
        "com.mydrama.sdk.session.$storageKey",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun read(nowEpochMillis: Long): StoredSession? {
        val token = preferences.getString("token", null) ?: return null
        val expiresAt = preferences.getLong("expires_at", 0L)
        return StoredSession(token, expiresAt).takeIf {
            it.expiresAtEpochMillis - REFRESH_SKEW_MILLIS > nowEpochMillis
        }
    }

    @Synchronized
    fun write(session: StoredSession) {
        check(
            preferences.edit()
                .putString("token", session.token)
                .putLong("expires_at", session.expiresAtEpochMillis)
                .commit(),
        ) { "Unable to persist the MyDrama session." }
    }

    @Synchronized
    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val REFRESH_SKEW_MILLIS = 30_000L
    }
}

internal data class StoredSession(
    val token: String,
    val expiresAtEpochMillis: Long,
)
