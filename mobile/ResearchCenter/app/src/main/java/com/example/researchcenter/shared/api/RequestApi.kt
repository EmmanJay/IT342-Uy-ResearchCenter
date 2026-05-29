package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.MaterialRequest
import retrofit2.Call
import retrofit2.http.*

interface RequestApi {
    @POST("repositories/{repoId}/requests")
    fun createRequest(@Path("repoId") repoId: Long, @Body body: Map<String, String>): Call<ApiResponse<MaterialRequest>>

    @POST("requests/{id}/fulfill")
    fun fulfillRequest(@Path("id") id: Long, @Body body: Map<String, Long>): Call<ApiResponse<MaterialRequest>>

    @PUT("requests/{id}/material")
    fun updateFulfillment(@Path("id") id: Long, @Body body: Map<String, Long>): Call<ApiResponse<MaterialRequest>>

    @DELETE("requests/{id}")
    fun deleteRequest(@Path("id") id: Long): Call<ApiResponse<Any>>
}
