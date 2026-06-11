package com.mydrama.sdk

import org.junit.Assert.assertThrows
import org.junit.After
import org.junit.Test

class MyDramaSDKTest {
    @After
    fun tearDown() {
        MyDramaSDK.resetForTests()
    }

    @Test
    fun getClientRequiresInitialization() {
        assertThrows(IllegalStateException::class.java) {
            MyDramaSDK.getClient()
        }
    }
}
