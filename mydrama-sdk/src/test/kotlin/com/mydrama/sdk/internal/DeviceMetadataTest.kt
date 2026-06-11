package com.mydrama.sdk.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMetadataTest {
    @Test
    fun headersMatchWorkerContractAndExposeSdkUserAgent() {
        val metadata = DeviceMetadata(
            packageName = "com.example.app",
            appVersion = "2.4.0",
            sdkVersion = "1.0.0",
            androidApiLevel = 35,
            manufacturer = "Example",
            model = "Phone",
            localeTag = "en-US",
        )

        val headers = metadata.headers(1234)

        assertEquals("com.example.app", headers["X-App-Package-ID"])
        assertEquals("1234", headers["X-App-Elapsed-Realtime-MS"])
        assertTrue(headers.getValue("User-Agent").startsWith("MyDrama-Android-SDK/1.0.0"))
        assertFalse(headers.values.any { it.contains("sk_live") })
    }
}
