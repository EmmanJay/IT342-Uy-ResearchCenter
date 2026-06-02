package com.example.researchcenter.shared.model

data class RepositoryUpdate(
    val id: Long,
    val repositoryId: Long,
    val content: String,
    val authorId: Long,
    val authorName: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val authorProfilePicture: String? = null,
    val authorRole: String? = null
)
