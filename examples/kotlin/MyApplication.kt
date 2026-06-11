package com.example.app

import android.app.Application
import com.mydrama.sdk.MyDramaConfig
import com.mydrama.sdk.MyDramaSDK
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val client = MyDramaSDK.initialize(
            this,
            BuildConfig.MYDRAMA_API_KEY,
            MyDramaConfig.build {
                baseUrl(BuildConfig.MYDRAMA_BASE_URL)
            },
        )

        client.newCall("api/home?lang=en").enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) = Unit

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    // Parse the response on this background callback.
                }
            }
        })
    }
}
