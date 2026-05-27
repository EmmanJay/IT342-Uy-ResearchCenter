package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MaterialApi {
    @GET("materials")
    fun getMaterials(): Call<Any>

    @POST("materials")
    fun createMaterial(@Body body: RequestBody): Call<ApiResponse<Material>>
}
