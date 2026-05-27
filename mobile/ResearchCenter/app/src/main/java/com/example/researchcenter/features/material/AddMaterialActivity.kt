package com.example.researchcenter.features.material

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddMaterialActivity : Activity() {

    private val typeOptions = listOf("LINK", "PDF", "REFERENCE")
    private var selectedMaterialType = "LINK"
    
    private lateinit var tvFormTitle: TextView
    private lateinit var actMaterialType: AutoCompleteTextView
    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var containerUrl: LinearLayout
    private lateinit var etUrl: EditText
    private lateinit var containerIsbn: LinearLayout
    private lateinit var etIsbn: EditText
    private lateinit var containerPdf: LinearLayout
    private lateinit var etTags: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_material)

        val repoId = intent.getLongExtra("REPO_ID", 0)

        tvFormTitle = findViewById(R.id.tv_form_title)
        actMaterialType = findViewById(R.id.act_material_type)
        etTitle = findViewById(R.id.et_title)
        etDesc = findViewById(R.id.et_description)
        containerUrl = findViewById(R.id.container_url)
        etUrl = findViewById(R.id.et_url)
        containerIsbn = findViewById(R.id.container_isbn)
        etIsbn = findViewById(R.id.et_isbn)
        containerPdf = findViewById(R.id.container_pdf)
        etTags = findViewById(R.id.et_tags)
        tvError = findViewById(R.id.tv_metadata_loaded) // using as general error since we don't have tvError
        btnSave = findViewById(R.id.btn_save)

        actMaterialType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, typeOptions)
        )
        
        actMaterialType.setOnItemClickListener { _, _, position, _ ->
            applyMaterialType(typeOptions[position])
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
        btnSave.setOnClickListener { saveMaterial(repoId) }
    }

    private fun applyMaterialType(type: String) {
        selectedMaterialType = type
        tvFormTitle.text = when (type) {
            "REFERENCE" -> "Add Material (Reference)"
            "PDF" -> "Add Material (PDF)"
            else -> "Add Material (Link)"
        }

        containerUrl.visibility = if (type == "LINK") View.VISIBLE else View.GONE
        containerIsbn.visibility = if (type == "REFERENCE") View.VISIBLE else View.GONE
        containerPdf.visibility = if (type == "PDF") View.VISIBLE else View.GONE
    }

    private fun saveMaterial(repoId: Long) {
        val title = etTitle.text.toString().trim()
        val description = etDesc.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val tags = etTags.text.toString()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        val requestJson = JSONObject().apply {
            put("repositoryId", repoId)
            put("title", title)
            put("description", description)
            put("materialType", selectedMaterialType)
            put("tags", JSONArray(tags))
            put("status", "TO_READ")
            
            if (selectedMaterialType == "LINK") {
                put("url", etUrl.text.toString().trim())
            } else if (selectedMaterialType == "REFERENCE") {
                put("isbn", etIsbn.text.toString().trim())
            }
        }.toString()

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val materialApi = RetrofitClient.createService<MaterialApi>()
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())

        materialApi.createMaterial(requestBody).enqueue(object : Callback<ApiResponse<Material>> {
            override fun onResponse(call: Call<ApiResponse<Material>>, response: Response<ApiResponse<Material>>) {
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Material"
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AddMaterialActivity, "Material added!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMaterialActivity, "Failed to save material.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Material"
                    Toast.makeText(this@AddMaterialActivity, "Network error.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
