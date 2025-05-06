package com.example.aiavatargenerator.data.api

import com.example.aiavatargenerator.data.model.AvatarRequest
import com.example.aiavatargenerator.data.model.AvatarResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AvatarApi {
    @POST("/generate-avatar")
    fun generateAvatar(@Body request: PromptRequest): Call<AvatarResponse>
}

data class PromptRequest(val prompt: String)
data class AvatarResponse(val imageBase64: String)

