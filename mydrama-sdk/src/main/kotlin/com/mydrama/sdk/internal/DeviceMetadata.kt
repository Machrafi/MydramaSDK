package com.mydrama.sdk.internal

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.mydrama.sdk.BuildConfig
import java.util.Locale

internal data class DeviceMetadata(
    val packageName: String,
    val appVersion: String,
    val sdkVersion: String,
    val androidApiLevel: Int,
    val manufacturer: String,
    val model: String,
    val localeTag: String,
) {
    val userAgent: String
        get() = "MyDrama-Android-SDK/$sdkVersion " +
            "($packageName/$appVersion; Android $androidApiLevel; " +
            "${sanitize(manufacturer)} ${sanitize(model)}; $localeTag)"

    fun headers(elapsedRealtimeMillis: Long): Map<String, String> = linkedMapOf(
        "X-App-Package-ID" to packageName,
        "X-App-Elapsed-Realtime-MS" to elapsedRealtimeMillis.toString(),
        "X-MyDrama-SDK-Version" to sdkVersion,
        "X-App-Version" to appVersion,
        "X-Android-API-Level" to androidApiLevel.toString(),
        "X-Device-Manufacturer" to sanitize(manufacturer),
        "X-Device-Model" to sanitize(model),
        "X-App-Locale" to localeTag,
        "User-Agent" to userAgent,
    )

    companion object {
        fun collect(context: Context): DeviceMetadata {
            val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
            val locale = if (Build.VERSION.SDK_INT >= 24) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            } ?: Locale.getDefault()

            return DeviceMetadata(
                packageName = context.packageName,
                appVersion = packageInfo.versionName ?: "unknown",
                sdkVersion = BuildConfig.SDK_VERSION,
                androidApiLevel = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "unknown" },
                model = Build.MODEL.orEmpty().ifBlank { "unknown" },
                localeTag = locale.toLanguageTag().ifBlank { "und" },
            )
        }

        @Suppress("DEPRECATION")
        private fun android.content.pm.PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
            if (Build.VERSION.SDK_INT >= 33) {
                getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                getPackageInfo(packageName, 0)
            }
    }
}

private fun sanitize(value: String): String =
    value.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80)
