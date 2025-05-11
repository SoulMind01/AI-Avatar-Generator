package com.example.aiavatargenerator.data.api

import com.example.aiavatargenerator.data.model.AvatarResponse
import com.example.aiavatargenerator.network.AnimateRequest
import com.example.aiavatargenerator.network.AnimateResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AvatarApi {
    @POST("/generate-avatar")
    fun generateAvatar(@Body request: PromptRequest): Call<AvatarResponse>
    @POST("/generate-video")
    fun generateVideo(@Body request: PromptRequest): Call<VideoResponse>

    data class VideoResponse(val videoBase64: String)

}

data class PromptRequest(val prompt: String)
data class AvatarResponse(val imageBase64: String)

