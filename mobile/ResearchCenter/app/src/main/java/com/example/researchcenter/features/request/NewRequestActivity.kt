package com.example.researchcenter.features.request

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.api.RequestApi
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.MaterialRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_request)

        val repoId = intent.getLongExtra("REPO_ID", 0)
        val etTitle = findViewById<EditText>(R.id.et_title)
        val etDesc = findViewById<EditText>(R.id.et_description)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val btnCreate = findViewById<Button>(R.id.btn_create)

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                tvError.text = "Title is required."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = View.GONE
            btnCreate.isEnabled = false
            btnCreate.text = "Creating..."

            val requestApi = RetrofitClient.createService<RequestApi>()
            val body = mapOf(
                "repositoryId" to repoId,
                "title" to title,
                "description" to etDesc.text.toString().trim()
            )

            requestApi.createRequest(body).enqueue(object : Callback<ApiResponse<MaterialRequest>> {
                override fun onResponse(call: Call<ApiResponse<MaterialRequest>>, response: Response<ApiResponse<MaterialRequest>>) {
                    runOnUiThread {
                        btnCreate.isEnabled = true
                        btnCreate.text = "Create Request"
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@NewRequestActivity, "Request created!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            tvError.text = "Failed to create request."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialRequest>>, t: Throwable) {
                    runOnUiThread {
                        btnCreate.isEnabled = true
                        btnCreate.text = "Create Request"
                        tvError.text = "Network error."
                        tvError.visibility = View.VISIBLE
                    }
                }
            })
        }
    }
}
