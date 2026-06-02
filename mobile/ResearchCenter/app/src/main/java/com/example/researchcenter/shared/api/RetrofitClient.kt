package com.example.researchcenter.shared.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiClient.BASE_URL)
            .client(ApiClient.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val client: okhttp3.OkHttpClient
        get() = ApiClient.client

    inline fun <reified T> createService(): T {
        return retrofit.create(T::class.java)
    }
}
