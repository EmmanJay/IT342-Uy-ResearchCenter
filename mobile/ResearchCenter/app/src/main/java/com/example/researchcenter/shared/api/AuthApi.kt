package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @GET("users/me")
    fun getMe(): Call<ApiResponse<UserData>>

    @PUT("users/me")
    fun updateProfile(@Body req: UpdateProfileRequest): Call<ApiResponse<UserData>>

    @POST("auth/login")
    fun login(@Body req: LoginRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/register")
    fun register(@Body req: RegisterRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/google")
    fun googleAuth(@Body req: GoogleAuthRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    fun refreshToken(@Body req: RefreshTokenRequest): Call<ApiResponse<AuthResponse>>
}
