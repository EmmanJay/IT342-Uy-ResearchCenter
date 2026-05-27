package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ActivityLog
import com.example.researchcenter.shared.model.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ActivityApi {
    @GET("activity")
    fun getActivity(): Call<Any>

    @GET("activities")
    fun getActivities(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<List<ActivityLog>>>

    @GET("activities")
    fun getRepositoryActivities(
        @Query("repositoryId") repositoryId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Call<ApiResponse<List<ActivityLog>>>
}
