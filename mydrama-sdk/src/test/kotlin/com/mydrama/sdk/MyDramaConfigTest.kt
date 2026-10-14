package com.mydrama.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MyDramaConfigTest {
    @Test
    fun builderDefaultsToOfficialApi() {
        assertEquals("https://sdk.dramasdk.com/", MyDramaConfig.Builder().build().baseUrl.toString())
    }

    @Test
    fun baseUrlAddsTrailingSlash() {
        val config = MyDramaConfig.build {
            baseUrl("https://example.com/worker")
        }

        assertEquals("https://example.com/worker/", config.baseUrl.toString())
    }

    @Test
    fun rejectsInsecureRemoteBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            MyDramaConfig.build { baseUrl("http://example.com") }
        }
    }

    @Test
    fun permitsLocalHttpForDevelopment() {
        val config = MyDramaConfig.build { baseUrl("http://localhost:8787") }
        assertEquals("http://localhost:8787/", config.baseUrl.toString())
    }
    @Test
    fun rejectsCredentialsAndQueryInBaseUrl() {
        for (url in listOf("https://user:pass@example.com", "https://example.com?key=secret", "https://example.com/#fragment")) {
            assertThrows(IllegalArgumentException::class.java) { MyDramaConfig.build { baseUrl(url) } }
        }
    }

}
