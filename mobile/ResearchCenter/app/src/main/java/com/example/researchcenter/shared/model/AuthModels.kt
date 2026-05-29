package com.example.researchcenter.shared.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstname: String,
    val lastname: String
)

data class AuthResponse(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    @com.google.gson.annotations.SerializedName("accessToken") val accessToken: String,
    @com.google.gson.annotations.SerializedName("refreshToken") val refreshToken: String,
    val profilePicture: String? = null
)

data class GoogleAuthRequest(
    val idToken: String
)

data class UpdateProfileRequest(
    val firstname: String,
    val lastname: String,
    val profilePicture: String? = null
)

data class RefreshTokenRequest(
    val refreshToken: String
)

/** @deprecated Legacy helper for ApiClient */
data class LoginResponse(
    @com.google.gson.annotations.SerializedName("accessToken") val accessToken: String,
    @com.google.gson.annotations.SerializedName("refreshToken") val refreshToken: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val timestamp: String? = null
)

data class ApiError(
    val code: String?,
    val message: String?
)

data class AuthData(
    val user: UserData,
    val accessToken: String,
    val refreshToken: String
)

data class UserData(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    val profilePicture: String? = null,
    val createdAt: String? = null
)
