package com.example.researchcenter.model

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

data class LoginResponse(
    val token: String,
    val refreshToken: String
)

data class RegisterResponse(
    val token: String,
    val refreshToken: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val timestamp: String
)

data class ApiError(
    val code: String,
    val message: String
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
    val role: String
)
