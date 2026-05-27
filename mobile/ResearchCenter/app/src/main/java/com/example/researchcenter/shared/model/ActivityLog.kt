package com.example.researchcenter.shared.model

data class ActivityLog(
    val id: Long = 0,
    val userId: Long = 0,
    val repositoryId: Long? = null,
    val action: String = "",
    val details: String = "",
    val timestamp: String = ""
)
