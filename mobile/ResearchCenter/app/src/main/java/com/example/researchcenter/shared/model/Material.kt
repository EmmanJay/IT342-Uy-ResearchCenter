package com.example.researchcenter.shared.model

data class Material(
    val id: Long,
    val repositoryId: Long,
    val title: String,
    val description: String?,
    val materialType: String,
    val fileUrl: String?,
    val url: String?,
    val uploaderId: Long,
    val uploaderName: String,
    val tags: List<String>,
    val status: String,
    val createdAt: String,
    val authors: String? = null,
    val publisher: String? = null,
    val year: String? = null,
    val isbn: String? = null,
    val metadata: String? = null,
    val myStatus: String? = null,
    val bookmarked: Boolean = false
)
