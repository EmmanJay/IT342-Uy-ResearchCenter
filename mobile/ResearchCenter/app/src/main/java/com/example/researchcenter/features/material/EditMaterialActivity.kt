package com.example.researchcenter.features.material

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import com.example.researchcenter.shared.ui.BreadcrumbsView
import com.example.researchcenter.shared.ui.UserAvatarView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.URLEncoder

class EditMaterialActivity : AppCompatActivity() {

    private var materialId: Long = -1
    private var repoId: Long = -1
    private var repoName: String = "Repository"
    private var selectedMaterialType = "LINK"

    private lateinit var tvFormTitle: TextView
    private lateinit var actMaterialType: AutoCompleteTextView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    
    private lateinit var containerUrl: LinearLayout
    private lateinit var etUrl: TextInputEditText
    
    private lateinit var containerIsbn: LinearLayout
    private lateinit var etIsbn: TextInputEditText
    
    private lateinit var containerPdf: LinearLayout
    private lateinit var tvFileName: TextView
    
    private lateinit var containerGoogleSearch: LinearLayout
    private lateinit var etSearchBooks: TextInputEditText
    private lateinit var btnSearchBooks: MaterialButton
    private lateinit var tvMetadataLoaded: TextView
    
    private lateinit var etTags: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var breadcrumbs: BreadcrumbsView
    private lateinit var tvAvatar: UserAvatarView

    // Reference properties
    private var fetchedAuthors: String? = null
    private var fetchedPublisher: String? = null
    private var fetchedYear: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_material)

        materialId = intent.getLongExtra("MATERIAL_ID", -1L)
        repoId = intent.getLongExtra("REPO_ID", -1L)
        repoName = intent.getStringExtra("REPO_NAME") ?: "Repository"

        if (materialId == -1L) {
            Toast.makeText(this, "Invalid Material ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupTopBar()
        setupListeners()
        loadMaterialDetails()
    }

    private fun initViews() {
        tvFormTitle = findViewById(R.id.tv_form_title)
        actMaterialType = findViewById(R.id.act_material_type)
        etTitle = findViewById(R.id.et_title)
        etDesc = findViewById(R.id.et_description)
        
        containerUrl = findViewById(R.id.container_url)
        etUrl = findViewById(R.id.et_url)
        
        containerIsbn = findViewById(R.id.container_isbn)
        etIsbn = findViewById(R.id.et_isbn)
        
        containerPdf = findViewById(R.id.container_pdf)
        tvFileName = findViewById(R.id.tv_file_name)
        // Hide upload and remove actions for editing PDFs
        findViewById<View>(R.id.btn_upload_pdf).visibility = View.GONE
        findViewById<View>(R.id.btn_remove_file).visibility = View.GONE
        
        containerGoogleSearch = findViewById(R.id.container_google_search)
        etSearchBooks = findViewById(R.id.et_search_books)
        btnSearchBooks = findViewById(R.id.btn_search_books)
        tvMetadataLoaded = findViewById(R.id.tv_metadata_loaded)
        
        etTags = findViewById(R.id.et_tags)
        btnCancel = findViewById(R.id.btn_cancel)
        btnSave = findViewById(R.id.btn_save)
        breadcrumbs = findViewById(R.id.breadcrumbs)
        tvAvatar = findViewById(R.id.tv_avatar)

        tvFormTitle.text = "Edit Material"
        btnSave.text = "Save Changes"

        breadcrumbs.setPath(listOf(
            "Dashboard" to { finish() },
            repoName to { finish() },
            "Edit Material" to {}
        ))
    }

    private fun setupTopBar() {
        val name = SessionManager.getName(this)
        val email = SessionManager.getEmail(this)
        tvAvatar.setUser(name, email)
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveMaterial() }

        btnSearchBooks.setOnClickListener {
            val query = etSearchBooks.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGoogleBooks(query)
            }
        }
    }

    private fun loadMaterialDetails() {
        RetrofitClient.createService<MaterialApi>().getMaterial(materialId)
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(call: Call<ApiResponse<Material>>, response: Response<ApiResponse<Material>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { material ->
                            runOnUiThread {
                                populateMaterialDetails(material)
                            }
                        }
                    } else {
                        Toast.makeText(this@EditMaterialActivity, "Failed to load material details", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {
                    Toast.makeText(this@EditMaterialActivity, "Network error loading details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun populateMaterialDetails(material: Material) {
        selectedMaterialType = material.materialType
        etTitle.setText(material.title)
        etDesc.setText(material.description ?: "")
        etTags.setText(material.tags.joinToString(", "))

        actMaterialType.setText(material.materialType)
        actMaterialType.isEnabled = false // Disable editing the material type

        containerUrl.visibility = if (material.materialType == "LINK") View.VISIBLE else View.GONE
        containerIsbn.visibility = if (material.materialType == "REFERENCE") View.VISIBLE else View.GONE
        containerGoogleSearch.visibility = if (material.materialType == "REFERENCE") View.VISIBLE else View.GONE
        containerPdf.visibility = if (material.materialType == "PDF") View.VISIBLE else View.GONE

        if (material.materialType == "LINK") {
            etUrl.setText(material.url ?: "")
        } else if (material.materialType == "REFERENCE") {
            etIsbn.setText(material.isbn ?: "")
            fetchedAuthors = material.authors
            fetchedPublisher = material.publisher
            fetchedYear = material.year
        } else if (material.materialType == "PDF") {
            tvFileName.text = material.fileUrl?.substringAfterLast("/") ?: "Uploaded PDF File"
        }
    }

    private fun searchGoogleBooks(query: String) {
        tvMetadataLoaded.visibility = View.GONE
        btnSearchBooks.isEnabled = false
        btnSearchBooks.text = "Searching..."

        val url = "https://www.googleapis.com/books/v1/volumes?q=${URLEncoder.encode(query, "UTF-8")}&maxResults=5"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    btnSearchBooks.isEnabled = true
                    btnSearchBooks.text = "Search"
                    Toast.makeText(this@EditMaterialActivity, "Network error searching books", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                runOnUiThread {
                    btnSearchBooks.isEnabled = true
                    btnSearchBooks.text = "Search"
                }

                if (response.isSuccessful && !body.isNullOrBlank()) {
                    try {
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items")
                        if (items != null && items.length() > 0) {
                            val suggestions = mutableListOf<BookSuggestion>()
                            for (i in 0 until items.length()) {
                                val volumeInfo = items.getJSONObject(i).optJSONObject("volumeInfo") ?: continue
                                val title = volumeInfo.optString("title", "")
                                
                                val authorsArray = volumeInfo.optJSONArray("authors")
                                val authorsList = mutableListOf<String>()
                                if (authorsArray != null) {
                                    for (j in 0 until authorsArray.length()) {
                                        authorsList.add(authorsArray.getString(j))
                                    }
                                }
                                val firstAuthor = authorsList.firstOrNull() ?: "Unknown Author"
                                val authors = authorsList.joinToString(", ")
                                
                                val publisher = volumeInfo.optString("publisher", "")
                                val publishedDate = volumeInfo.optString("publishedDate", "")
                                val description = volumeInfo.optString("description", "")
                                
                                val industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers")
                                var isbn = ""
                                if (industryIdentifiers != null) {
                                    for (j in 0 until industryIdentifiers.length()) {
                                        val idObj = industryIdentifiers.getJSONObject(j)
                                        val type = idObj.optString("type")
                                        if (type == "ISBN_13" || type == "ISBN_10") {
                                            isbn = idObj.optString("identifier")
                                            if (type == "ISBN_13") break
                                        }
                                    }
                                }

                                val year = if (publishedDate.length >= 4) publishedDate.substring(0, 4) else publishedDate

                                suggestions.add(BookSuggestion(
                                    title = title,
                                    firstAuthor = firstAuthor,
                                    authors = authors,
                                    publisher = publisher,
                                    year = year,
                                    isbn = isbn,
                                    description = description
                                ))
                            }
                            runOnUiThread {
                                showBookSuggestionsDialog(suggestions)
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@EditMaterialActivity, "No books found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@EditMaterialActivity, "Error parsing book results", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun showBookSuggestionsDialog(suggestions: List<BookSuggestion>) {
        val titles = suggestions.map { "${it.title} by ${it.firstAuthor}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select a Book Suggestion")
            .setItems(titles) { _, which ->
                val chosen = suggestions[which]
                etTitle.setText(chosen.title)
                etDesc.setText(chosen.description ?: "")
                etIsbn.setText(chosen.isbn ?: "")
                
                fetchedAuthors = chosen.authors
                fetchedPublisher = chosen.publisher
                fetchedYear = chosen.year

                tvMetadataLoaded.visibility = View.VISIBLE
                Toast.makeText(this, "Book suggestion loaded!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMaterial() {
        val title = etTitle.text.toString().trim()
        val description = etDesc.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val tags = etTags.text.toString()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val bodyMap = mutableMapOf<String, Any?>()
        bodyMap["title"] = title
        bodyMap["description"] = description
        bodyMap["tags"] = tags

        if (selectedMaterialType == "LINK") {
            bodyMap["url"] = etUrl.text.toString().trim()
        } else if (selectedMaterialType == "REFERENCE") {
            bodyMap["isbn"] = etIsbn.text.toString().trim()
            bodyMap["authors"] = fetchedAuthors
            bodyMap["publisher"] = fetchedPublisher
            bodyMap["year"] = fetchedYear
        }

        RetrofitClient.createService<MaterialApi>().updateMaterial(materialId, bodyMap)
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(call: Call<ApiResponse<Material>>, response: Response<ApiResponse<Material>>) {
                    runOnUiThread {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Changes"
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@EditMaterialActivity, "Material updated successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@EditMaterialActivity, "Failed to update material", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {
                    runOnUiThread {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Changes"
                        Toast.makeText(this@EditMaterialActivity, "Network error updating material", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }
}
