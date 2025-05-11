package com.example.aiavatargenerator.network

import com.example.aiavatargenerator.data.api.AvatarApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()

    val api: AvatarApi by lazy {
        Retrofit.Builder()
            .baseUrl("YOUR_FLASK_BACKEND_URL") // Ensure trailing slash if needed
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(AvatarApi::class.java)
    }
}
