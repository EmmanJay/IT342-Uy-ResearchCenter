package com.example.researchcenter.shared.model


data class Repository(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val ownerName: String?,
    val role: String?,          // "OWNER" or "MEMBER" — your role in this repo
    val memberCount: Int,
    val materialCount: Int,
    val createdAt: String,
    val updatedAt: String? = null,
    val bookmarked: Boolean = false
)

data class RepositoryNote(
    val id: Long,
    val repositoryId: Long,
    val content: String,
    val authorId: Long,
    val authorName: String,
    val createdAt: String,
    val updatedAt: String? = null
)

data class BookmarkToggleResponse(
    val bookmarked: Boolean
)

data class RepositoryMember(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,           // "OWNER" or "MEMBER"
    val joinedAt: String
)
