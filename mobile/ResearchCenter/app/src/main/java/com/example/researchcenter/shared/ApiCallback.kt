package com.example.researchcenter.shared

import com.example.researchcenter.R
import com.example.researchcenter.shared.model.*
import com.example.researchcenter.shared.auth.*
import com.example.researchcenter.shared.api.*
import android.os.Bundle
import android.view.View
import android.widget.*
import android.content.Intent
import android.app.Activity
import androidx.recyclerview.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.LayoutInflater
import android.view.ViewGroup

interface ApiCallback<T> {
    fun onSuccess(data: T)
    fun onError(code: String, message: String)
}
