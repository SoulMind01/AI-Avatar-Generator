package com.example.aiavatargenerator.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AvatarApi {
    @POST("/generate-avatar")
    fun generateAvatar(@Body request: PromptRequest): retrofit2.Call<AvatarResponse>
}

data class PromptRequest(val prompt: String)
data class AvatarResponse(val imageBase64: String)

object RetrofitInstance {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: AvatarApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://d536-104-199-118-106.ngrok-free.app/") // Ensure trailing slash
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(AvatarApi::class.java)
    }
}
