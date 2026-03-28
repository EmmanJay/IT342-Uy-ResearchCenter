package com.example.researchcenter.model

data class User(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    val createdAt: String?
)
