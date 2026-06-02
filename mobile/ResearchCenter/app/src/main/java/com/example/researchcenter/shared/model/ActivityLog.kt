package com.example.researchcenter.shared.model

data class ActivityLog(
    val id: Long = 0,
    val userId: Long = 0,
    val actorName: String? = null,
    val actorProfilePicture: String? = null,
    val action: String = "",
    val targetType: String? = null,
    val targetId: Long? = null,
    val targetName: String? = null,
    val repositoryId: Long? = null,
    val repositoryName: String? = null,
    val targetUserId: Long? = null,
    val description: String? = null,
    val details: String = "",
    val timestamp: String = "",
    val createdAt: String? = null
)
