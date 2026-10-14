package com.mydrama.sdk.internal

import android.content.Context

/** Short-lived bearer tokens stay in memory and are never written into backups. */
internal class SessionStore(context: Context, storageKey: String) {
    private var current: StoredSession? = null

    init {
        // Remove plaintext session material left by SDK 1.0.x.
        context.getSharedPreferences("com.mydrama.sdk.session.$storageKey", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Synchronized
    fun read(nowEpochMillis: Long): StoredSession? {
        if (current?.let { it.expiresAtEpochMillis - 30_000L <= nowEpochMillis } == true) current = null
        return current
    }

    @Synchronized
    fun write(session: StoredSession) { current = session }

    @Synchronized
    fun clear() { current = null }
}

internal data class StoredSession(val token: String, val expiresAtEpochMillis: Long)
