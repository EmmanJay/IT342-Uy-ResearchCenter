package com.example.researchcenter.shared.model

data class User(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    val profilePicture: String? = null,
    val createdAt: String? = null
)
