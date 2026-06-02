package com.example.researchcenter.features.repository

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Repository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateRepositoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_repository)

        val etName = findViewById<EditText>(R.id.etName)
        val etDesc = findViewById<EditText>(R.id.etDescription)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnCreate = findViewById<Button>(R.id.btnCreate)

        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                tvError.text = "Name is required."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = View.GONE
            btnCreate.isEnabled = false
            btnCreate.text = "Creating..."

            val repositoryApi = RetrofitClient.createService<RepositoryApi>()
            val body = mapOf("name" to name, "description" to etDesc.text.toString().trim())

            repositoryApi.createRepository(body).enqueue(object : Callback<ApiResponse<Repository>> {
                override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                    val wrapper = response.body()
                    runOnUiThread {
                        btnCreate.isEnabled = true
                        btnCreate.text = "Create"
                        if (response.isSuccessful && wrapper?.success == true) {
                            Toast.makeText(this@CreateRepositoryActivity, "Repository created!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            tvError.text = wrapper?.error?.message ?: "Failed to create repository. Try again."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                    runOnUiThread {
                        btnCreate.isEnabled = true
                        btnCreate.text = "Create"
                        tvError.text = "Network error."
                        tvError.visibility = View.VISIBLE
                    }
                }
            })
        }
    }
}
