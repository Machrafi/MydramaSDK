package com.mydrama.sdk.internal

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InstallationIdStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("com.mydrama.sdk.installation", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun generatedIdIsStableAndWorkerCompatible() {
        val first = InstallationIdStore(context).getOrCreate()
        val second = InstallationIdStore(context).getOrCreate()

        assertEquals(first, second)
        assertTrue(first.length in 16..128)
        assertTrue(first.matches(Regex("^[A-Za-z0-9._:-]+$")))
    }
}
