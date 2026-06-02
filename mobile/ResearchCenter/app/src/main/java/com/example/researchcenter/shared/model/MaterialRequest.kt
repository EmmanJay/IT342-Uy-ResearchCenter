package com.example.researchcenter.shared.model

data class MaterialRequest(
    val id: Long,
    val repositoryId: Long,
    val title: String,
    val description: String?,
    val requesterId: Long,
    val requesterName: String,
    val status: String,         // "OPEN", "FULFILLED", "CLOSED", "CANCELLED"
    val fulfilledByName: String?,
    val materialId: Long?,
    val materialTitle: String?,
    val closureNote: String?,
    val createdAt: String
)
