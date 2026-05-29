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
    val requestCount: Int = 0,
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
    val profilePicture: String? = null,
    val joinedAt: String? = null
)

data class MaterialNote(
    val id: Long? = null,
    val materialId: Long? = null,
    val content: String,
    val userId: Long? = null
)

data class InvitePreview(
    val repositoryId: Long,
    val repositoryName: String,
    val status: String,
    val email: String
)

data class AcceptInviteResponse(
    val repositoryId: Long?,
    val message: String?
)

