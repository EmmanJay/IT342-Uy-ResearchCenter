package com.example.researchcenter.api

import com.example.researchcenter.model.ApiResponse
import com.example.researchcenter.model.LoginResponse
import com.example.researchcenter.model.UserData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/api/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }

    fun register(
        email: String,
        password: String,
        firstname: String,
        lastname: String,
        callback: ApiCallback<LoginResponse>
    ) {
        val body = gson.toJson(
            mapOf(
                "email" to email,
                "password" to password,
                "firstname" to firstname,
                "lastname" to lastname
            )
        ).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$BASE_URL/auth/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val wrapperType = object : TypeToken<ApiResponse<Map<String, Any>>>() {}.type
                        val wrapper: ApiResponse<Map<String, Any>> = gson.fromJson(responseBody, wrapperType)
                        
                        if (response.isSuccessful) {
                            if (wrapper.success && wrapper.data != null) {
                                val accessToken = wrapper.data["accessToken"] as? String
                                val refreshToken = wrapper.data["refreshToken"] as? String
                                if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                                    callback.onSuccess(LoginResponse(accessToken, refreshToken))
                                } else {
                                    callback.onError("Missing tokens in response")
                                }
                            } else {
                                callback.onError(wrapper.error?.code ?: "Registration failed")
                            }
                        } else {
                            // Handle non-2xx responses
                            if (wrapper.error != null) {
                                callback.onError(wrapper.error.code ?: "Error ${response.code}")
                            } else {
                                callback.onError("Error ${response.code}: ${responseBody.take(100)}")
                            }
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("Empty response from server")
                }
            }
        })
    }

    fun login(
        email: String,
        password: String,
        callback: ApiCallback<LoginResponse>
    ) {
        val body = gson.toJson(mapOf("email" to email, "password" to password))
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val wrapperType = object : TypeToken<ApiResponse<Map<String, Any>>>() {}.type
                        val wrapper: ApiResponse<Map<String, Any>> = gson.fromJson(responseBody, wrapperType)
                        
                        if (response.isSuccessful) {
                            if (wrapper.success && wrapper.data != null) {
                                val accessToken = wrapper.data["accessToken"] as? String
                                val refreshToken = wrapper.data["refreshToken"] as? String
                                if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                                    callback.onSuccess(LoginResponse(accessToken, refreshToken))
                                } else {
                                    callback.onError("Missing tokens in response")
                                }
                            } else {
                                callback.onError(wrapper.error?.code ?: "AUTH-001")
                            }
                        } else {
                            // Handle non-2xx responses
                            if (wrapper.error != null) {
                                callback.onError(wrapper.error.code ?: "Error ${response.code}")
                            } else {
                                callback.onError("Error ${response.code}")
                            }
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("Empty response from server")
                }
            }
        })
    }

    fun getMe(token: String, callback: ApiCallback<UserData>) {
        val request = Request.Builder()
            .url("$BASE_URL/users/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val wrapperType = object : TypeToken<ApiResponse<Map<String, Any>>>() {}.type
                        val wrapper: ApiResponse<Map<String, Any>> = gson.fromJson(responseBody, wrapperType)
                        if (wrapper.success && wrapper.data != null) {
                            val data = wrapper.data
                            val user = UserData(
                                id = (data["id"] as? Double)?.toLong() ?: 0L,
                                email = (data["email"] as? String) ?: "",
                                firstname = (data["firstname"] as? String) ?: "",
                                lastname = (data["lastname"] as? String) ?: "",
                                role = (data["role"] as? String) ?: ""
                            )
                            callback.onSuccess(user)
                        } else {
                            callback.onError("Failed to get user")
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("Error ${response.code}")
                }
            }
        })
    }
}
