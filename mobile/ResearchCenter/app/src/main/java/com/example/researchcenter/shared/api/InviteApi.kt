package com.example.researchcenter.shared.api

import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.InvitePreview
import com.example.researchcenter.shared.model.AcceptInviteResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InviteApi {
    @GET("invitations/{token}")
    fun getInvitation(@Path("token") token: String): Call<ApiResponse<InvitePreview>>

    @POST("invitations/{token}/accept")
    fun acceptInvitation(@Path("token") token: String): Call<ApiResponse<AcceptInviteResponse>>

    @POST("invitations/{token}/reject")
    fun rejectInvitation(@Path("token") token: String): Call<ApiResponse<Any>>
}
