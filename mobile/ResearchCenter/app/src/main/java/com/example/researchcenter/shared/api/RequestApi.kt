package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.MaterialRequest
import retrofit2.Call
import retrofit2.http.*

interface RequestApi {
    @POST("requests")
    fun createRequest(@Body body: Map<String, Any>): Call<ApiResponse<MaterialRequest>>
}
