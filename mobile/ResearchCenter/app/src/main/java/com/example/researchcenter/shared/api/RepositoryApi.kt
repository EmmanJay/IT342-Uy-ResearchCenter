package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.BookmarkToggleResponse
import com.example.researchcenter.shared.model.Repository
import retrofit2.Call
import retrofit2.http.*

interface RepositoryApi {
    @GET("repositories/{id}")
    fun getRepository(@Path("id") id: Long): Call<ApiResponse<Repository>>

    @GET("repositories")
    fun getRepositories(): Call<ApiResponse<List<Repository>>>

    @POST("repositories")
    fun createRepository(@Body body: Map<String, String>): Call<ApiResponse<Repository>>

    @PUT("repositories/{id}")
    fun updateRepository(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<Repository>>

    @DELETE("repositories/{id}")
    fun deleteRepository(@Path("id") id: Long): Call<ApiResponse<Any>>

    @POST("repositories/{id}/bookmark")
    fun toggleBookmark(@Path("id") id: Long): Call<ApiResponse<BookmarkToggleResponse>>

    @GET("repositories/{id}/materials")
    fun getMaterials(@Path("id") id: Long): Call<ApiResponse<List<com.example.researchcenter.shared.model.Material>>>

    @GET("repositories/{id}/requests")
    fun getRequests(@Path("id") id: Long): Call<ApiResponse<List<com.example.researchcenter.shared.model.MaterialRequest>>>
    
    @GET("repositories/{id}/members")
    fun getMembers(@Path("id") id: Long): Call<ApiResponse<List<com.example.researchcenter.shared.model.RepositoryMember>>>
    
    @POST("repositories/{id}/invite")
    fun inviteMember(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<Any>>

    @DELETE("repositories/{id}/members/{userId}")
    fun removeMember(@Path("id") id: Long, @Path("userId") userId: String): Call<ApiResponse<Any>>

    @GET("repositories/{id}/updates")
    fun getUpdates(@Path("id") id: Long, @Query("page") page: Int = 0, @Query("size") size: Int = 50): Call<ApiResponse<List<com.example.researchcenter.shared.model.RepositoryUpdate>>>
}
