package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.*
import retrofit2.Call
import retrofit2.http.*

interface UserApi {
    @GET("users/me")
    fun getMe(): Call<ApiResponse<UserData>>

    @PUT("users/me")
    fun updateProfile(@Body req: UpdateProfileRequest): Call<ApiResponse<UserData>>

    @GET("users/search")
    fun searchUsers(@Query("email") email: String): Call<ApiResponse<List<User>>>
}
