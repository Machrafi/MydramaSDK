package com.example.app;

import android.app.Application;

import com.mydrama.sdk.MyDramaClient;
import com.mydrama.sdk.MyDramaConfig;
import com.mydrama.sdk.MyDramaSDK;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public final class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MyDramaConfig config = MyDramaConfig.builder()
                .baseUrl(BuildConfig.MYDRAMA_BASE_URL)
                .build();
        MyDramaClient client = MyDramaSDK.initialize(
                this,
                BuildConfig.MYDRAMA_API_KEY,
                config
        );

        client.newCall("api/home?lang=en").enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                // Handle the network error.
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
            }
        });
    }
}
