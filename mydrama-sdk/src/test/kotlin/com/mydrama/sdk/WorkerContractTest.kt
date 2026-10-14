package com.mydrama.sdk

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WorkerContractTest {
    private lateinit var worker: MockWebServer
    private lateinit var external: MockWebServer
    private lateinit var context: Context

    @Before
    fun setUp() {
        worker = MockWebServer()
        external = MockWebServer()
        worker.start()
        external.start()
        context = RuntimeEnvironment.getApplication()
        MyDramaSDK.resetForTests()
    }

    @After
    fun tearDown() {
        MyDramaSDK.resetForTests()
        worker.shutdown()
        external.shutdown()
    }

    @Test
    fun registrationAndInterceptorMatchWorkerContractWithoutCrossOriginLeakage() {
        worker.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "registered": true,
                      "sessionToken": "session-jwt",
                      "expiresIn": 900,
                      "mode": "review",
                      "provider": "d1",
                      "gatePassed": false,
                      "riskLevel": "normal"
                    }
                    """.trimIndent(),
                ),
        )
        worker.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        external.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val config = MyDramaConfig.build {
            baseUrl(worker.url("/").toString())
            registerSessionOnInitialize(false)
        }
        val client = MyDramaSDK.initialize(context, "sk_live_contract_test", config)

        val registration = client.registerSession()
        assertEquals("session-jwt", registration.sessionToken)
        assertEquals("review", registration.mode)

        val registrationRequest = worker.takeRequest()
        assertEquals("/api/session/register", registrationRequest.path)
        assertEquals("POST", registrationRequest.method)
        assertEquals(context.packageName, registrationRequest.getHeader("X-App-Package-ID"))
        assertEquals("sk_live_contract_test", registrationRequest.getHeader("X-App-Secure-Token"))
        assertNotNull(registrationRequest.getHeader("X-App-Instance-ID"))
        assertNotNull(registrationRequest.getHeader("X-App-Elapsed-Realtime-MS"))
        assertTrue(
            registrationRequest.getHeader("User-Agent")
                .orEmpty()
                .startsWith("MyDrama-Android-SDK/"),
        )
        assertEquals("{}", registrationRequest.body.readUtf8())

        client.newCall("api/home?lang=en").execute().use {
            assertTrue(it.isSuccessful)
        }
        val apiRequest = worker.takeRequest()
        assertEquals("Bearer session-jwt", apiRequest.getHeader("Authorization"))
        assertEquals("sk_live_contract_test", apiRequest.getHeader("X-App-Secure-Token"))

        val externalRequest = Request.Builder().url(external.url("/public")).build()
        client.okHttpClient.newCall(externalRequest).execute().use {
            assertTrue(it.isSuccessful)
        }
        val leakedRequest = external.takeRequest()
        assertNull(leakedRequest.getHeader("Authorization"))
        assertNull(leakedRequest.getHeader("X-App-Secure-Token"))
        assertNull(leakedRequest.getHeader("X-App-Package-ID"))
        assertFalse(leakedRequest.headers.toString().contains("sk_live_contract_test"))
    }
    @Test
    fun redirectsCannotCarrySdkCredentialsToAnotherOrigin() {
        worker.enqueue(MockResponse().setResponseCode(302).setHeader("Location", external.url("/capture")))
        external.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = MyDramaSDK.initialize(context, "test-secret", MyDramaConfig.build {
            baseUrl(worker.url("/").toString()); registerSessionOnInitialize(false)
        })
        val sharedClient = client.installOn(OkHttpClient.Builder()).build()
        sharedClient.newCall(Request.Builder().url(worker.url("/api/home")).build()).execute().close()
        assertEquals("test-secret", worker.takeRequest().getHeader("X-App-Secure-Token"))
        val redirected = external.takeRequest()
        assertNull(redirected.getHeader("X-App-Secure-Token"))
        assertNull(redirected.getHeader("X-App-Package-ID"))
        assertNull(redirected.getHeader("X-App-Instance-ID"))
        assertNull(redirected.getHeader("Authorization"))
    }

    @Test
    fun sdkCallsRejectExternalOriginsAndEncodeStoryIds() {
        val client = MyDramaSDK.initialize(context, "test-secret", MyDramaConfig.build {
            baseUrl(worker.url("/").toString()); registerSessionOnInitialize(false)
        })
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { client.newCall(external.url("/api/home").toString()) }
        assertEquals("/api/chapters/id%2Fwith%3Fspecial", client.chapters("id/with?special").request().url.encodedPath)
        assertEquals("id&episode=999", client.stream("id&episode=999").request().url.queryParameter("bookId"))
        assertEquals("1", client.stream("id&episode=999").request().url.queryParameter("episode"))
    }

}
