package com.mydrama.sdk.internal

import android.content.Context
import java.util.UUID

internal class InstallationIdStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreate(): String {
        preferences.getString(INSTALLATION_ID_KEY, null)
            ?.takeIf(::isValid)
            ?.let { return it }

        val generated = UUID.randomUUID().toString()
        check(preferences.edit().putString(INSTALLATION_ID_KEY, generated).commit()) {
            "Unable to persist the MyDrama installation ID."
        }
        return generated
    }

    private fun isValid(value: String): Boolean =
        value.length in 16..128 && value.matches(Regex("^[A-Za-z0-9._:-]+$"))

    companion object {
        private const val PREFERENCES_NAME = "com.mydrama.sdk.installation"
        private const val INSTALLATION_ID_KEY = "installation_id"
    }
}
