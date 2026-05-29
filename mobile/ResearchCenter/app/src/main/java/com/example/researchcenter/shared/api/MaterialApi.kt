package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface MaterialApi {
    @GET("materials/bookmarked")
    fun getBookmarkedMaterials(): Call<ApiResponse<List<Material>>>

    @GET("materials/{id}")
    fun getMaterial(@Path("id") id: Long): Call<ApiResponse<Material>>

    @POST("materials")
    fun createMaterial(@Body body: Map<String, Any?>): Call<ApiResponse<Material>>

    @Multipart
    @POST("materials/upload")
    fun uploadMaterial(
        @Query("repositoryId") repositoryId: Long,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("tags") tags: RequestBody?
    ): Call<ApiResponse<Material>>

    @PUT("materials/{id}")
    fun updateMaterial(@Path("id") id: Long, @Body body: Map<String, Any?>): Call<ApiResponse<Material>>

    @DELETE("materials/{id}")
    fun deleteMaterial(@Path("id") id: Long): Call<ApiResponse<Any>>

    @PATCH("materials/{id}/status")
    fun updateStatus(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<Material>>

    @POST("materials/{id}/bookmark")
    fun toggleBookmark(@Path("id") id: Long): Call<ApiResponse<BookmarkToggleResponse>>

    @GET("materials/{id}/note")
    fun getNote(@Path("id") id: Long): Call<ApiResponse<MaterialNote>>

    @PUT("materials/{id}/note")
    fun saveNote(@Path("id") id: Long, @Body body: Map<String, String>): Call<ApiResponse<MaterialNote>>
}
