package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.*
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

    @POST("repositories/{id}/leave")
    fun leaveRepository(@Path("id") id: Long): Call<ApiResponse<Any>>

    // Materials
    @GET("repositories/{id}/materials")
    fun getMaterials(@Path("id") id: Long): Call<ApiResponse<List<Material>>>

    // Requests
    @GET("repositories/{id}/requests")
    fun getRequests(@Path("id") id: Long): Call<ApiResponse<List<MaterialRequest>>>

    // Members
    @GET("repositories/{id}/members")
    fun getMembers(@Path("id") id: Long): Call<ApiResponse<List<RepositoryMember>>>

    @POST("repositories/{id}/invite")
    fun inviteMember(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<Any>>

    @DELETE("repositories/{id}/members/{userId}")
    fun removeMember(@Path("id") id: Long, @Path("userId") userId: Long): Call<ApiResponse<Any>>

    // Updates
    @GET("repositories/{id}/updates")
    fun getUpdates(
        @Path("id") id: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Call<ApiResponse<List<RepositoryUpdate>>>

    @POST("repositories/{id}/updates")
    fun createUpdate(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<RepositoryUpdate>>

    @PUT("repositories/{id}/updates/{updateId}")
    fun editUpdate(
        @Path("id") id: Long,
        @Path("updateId") updateId: Long,
        @Body body: Map<String, String>
    ): Call<ApiResponse<RepositoryUpdate>>

    @DELETE("repositories/{id}/updates/{updateId}")
    fun deleteUpdate(@Path("id") id: Long, @Path("updateId") updateId: Long): Call<ApiResponse<Any>>
}
