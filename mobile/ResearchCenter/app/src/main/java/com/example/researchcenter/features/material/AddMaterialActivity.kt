package com.example.researchcenter.features.material

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.URLEncoder

class AddMaterialActivity : AppCompatActivity() {

    private val typeOptions = listOf("LINK", "PDF", "REFERENCE")
    private var selectedMaterialType = "LINK"

    private var repoId: Long = -1
    private var repoName: String = "Repository"

    private lateinit var tvFormTitle: TextView
    private lateinit var actMaterialType: AutoCompleteTextView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    
    private lateinit var containerUrl: LinearLayout
    private lateinit var etUrl: TextInputEditText
    
    private lateinit var containerIsbn: LinearLayout
    private lateinit var etIsbn: TextInputEditText
    
    private lateinit var containerPdf: LinearLayout
    private lateinit var btnUploadPdf: MaterialButton
    private lateinit var tvFileName: TextView
    private lateinit var btnRemoveFile: ImageButton
    
    private lateinit var containerGoogleSearch: LinearLayout
    private lateinit var etSearchBooks: TextInputEditText
    private lateinit var btnSearchBooks: MaterialButton
    private lateinit var tvMetadataLoaded: TextView
    
    private lateinit var etTags: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var breadcrumbs: BreadcrumbsView
    private lateinit var tvAvatar: UserAvatarView

    // Selected PDF file info
    private var selectedPdfUri: Uri? = null
    private var selectedPdfBytes: ByteArray? = null
    private var selectedPdfName: String? = null

    // Google Books metadata storage
    private var fetchedAuthors: String? = null
    private var fetchedPublisher: String? = null
    private var fetchedYear: String? = null

    private val selectPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handlePdfSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_material)

        repoId = intent.getLongExtra("REPO_ID", -1L)
        repoName = intent.getStringExtra("REPO_NAME") ?: "Repository"

        if (repoId == -1L) {
            Toast.makeText(this, "Invalid Repository ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupTopBar()
        setupListeners()
        applyMaterialType("LINK")
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
        btnUploadPdf = findViewById(R.id.btn_upload_pdf)
        tvFileName = findViewById(R.id.tv_file_name)
        btnRemoveFile = findViewById(R.id.btn_remove_file)
        
        containerGoogleSearch = findViewById(R.id.container_google_search)
        etSearchBooks = findViewById(R.id.et_search_books)
        btnSearchBooks = findViewById(R.id.btn_search_books)
        tvMetadataLoaded = findViewById(R.id.tv_metadata_loaded)
        
        etTags = findViewById(R.id.et_tags)
        btnCancel = findViewById(R.id.btn_cancel)
        btnSave = findViewById(R.id.btn_save)
        breadcrumbs = findViewById(R.id.breadcrumbs)
        tvAvatar = findViewById(R.id.tv_avatar)

        breadcrumbs.setPath(listOf(
            "Dashboard" to { finish() },
            repoName to { finish() },
            "Add Material" to {}
        ))
    }

    private fun setupTopBar() {
        val name = SessionManager.getName(this)
        val email = SessionManager.getEmail(this)
        tvAvatar.setUser(name, email)
    }

    private fun setupListeners() {
        actMaterialType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, typeOptions)
        )
        
        actMaterialType.setOnItemClickListener { _, _, position, _ ->
            applyMaterialType(typeOptions[position])
        }

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveMaterial() }

        btnUploadPdf.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        btnRemoveFile.setOnClickListener {
            clearSelectedPdf()
        }

        btnSearchBooks.setOnClickListener {
            val query = etSearchBooks.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGoogleBooks(query)
            } else {
                Toast.makeText(this, "Please enter a book query to search", Toast.LENGTH_SHORT).show()
            }
        }
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
        containerGoogleSearch.visibility = if (type == "REFERENCE") View.VISIBLE else View.GONE
        containerPdf.visibility = if (type == "PDF") View.VISIBLE else View.GONE

        // Clear warning/status when changing type
        tvMetadataLoaded.visibility = View.GONE
    }

    private fun handlePdfSelected(uri: Uri) {
        selectedPdfUri = uri
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && nameIndex != -1) {
                selectedPdfName = c.getString(nameIndex)
            }
        }
        if (selectedPdfName == null) {
            selectedPdfName = uri.lastPathSegment ?: "Selected PDF"
        }

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                selectedPdfBytes = inputStream.readBytes()
            }

            val fileSize = selectedPdfBytes?.size ?: 0
            if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                Toast.makeText(this, "File size exceeds 10MB limit", Toast.LENGTH_LONG).show()
                clearSelectedPdf()
            } else {
                tvFileName.text = selectedPdfName
                btnRemoveFile.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load PDF file", Toast.LENGTH_SHORT).show()
            clearSelectedPdf()
        }
    }

    private fun clearSelectedPdf() {
        selectedPdfUri = null
        selectedPdfBytes = null
        selectedPdfName = null
        tvFileName.text = "No file selected"
        btnRemoveFile.visibility = View.GONE
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
                    Toast.makeText(this@AddMaterialActivity, "Network error searching books", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@AddMaterialActivity, "No books found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@AddMaterialActivity, "Error parsing book results", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AddMaterialActivity, "Google Books service error", Toast.LENGTH_SHORT).show()
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

        if (selectedMaterialType == "LINK") {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "URL is required for Link type", Toast.LENGTH_SHORT).show()
                return
            }
            saveNormalMaterial(title, description, tags, url, null)
        } else if (selectedMaterialType == "REFERENCE") {
            val isbn = etIsbn.text.toString().trim()
            saveNormalMaterial(title, description, tags, null, isbn)
        } else if (selectedMaterialType == "PDF") {
            if (selectedPdfBytes == null) {
                Toast.makeText(this, "Please select a PDF file to upload", Toast.LENGTH_SHORT).show()
                return
            }
            uploadPdfMaterial(title, description, tags)
        }
    }

    private fun saveNormalMaterial(
        title: String,
        description: String,
        tags: List<String>,
        url: String?,
        isbn: String?
    ) {
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val bodyMap = mutableMapOf<String, Any?>()
        bodyMap["repositoryId"] = repoId
        bodyMap["title"] = title
        bodyMap["description"] = description
        bodyMap["materialType"] = selectedMaterialType
        bodyMap["tags"] = tags
        bodyMap["status"] = "TO_READ"

        if (selectedMaterialType == "LINK") {
            bodyMap["url"] = url
        } else if (selectedMaterialType == "REFERENCE") {
            bodyMap["isbn"] = isbn
            bodyMap["authors"] = fetchedAuthors
            bodyMap["publisher"] = fetchedPublisher
            bodyMap["year"] = fetchedYear
        }

        RetrofitClient.createService<MaterialApi>().createMaterial(bodyMap)
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(call: Call<ApiResponse<Material>>, response: Response<ApiResponse<Material>>) {
                    runOnUiThread {
                        btnSave.isEnabled = true
                        btnSave.text = "Add Material"
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddMaterialActivity, "Material saved successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@AddMaterialActivity, "Failed to save material", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {
                    runOnUiThread {
                        btnSave.isEnabled = true
                        btnSave.text = "Add Material"
                        Toast.makeText(this@AddMaterialActivity, "Network error saving material", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun uploadPdfMaterial(
        title: String,
        description: String,
        tags: List<String>
    ) {
        btnSave.isEnabled = false
        btnSave.text = "Uploading..."

        val titleBody = title.toRequestBody("text/plain".toMediaType())
        val descBody = description.toRequestBody("text/plain".toMediaType())
        val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaType())

        val filePart = MultipartBody.Part.createFormData(
            "file",
            selectedPdfName ?: "material.pdf",
            selectedPdfBytes!!.toRequestBody("application/pdf".toMediaType())
        )

        RetrofitClient.createService<MaterialApi>().uploadMaterial(
            repositoryId = repoId,
            file = filePart,
            title = titleBody,
            description = descBody,
            tags = tagsBody
        ).enqueue(object : Callback<ApiResponse<Material>> {
            override fun onResponse(call: Call<ApiResponse<Material>>, response: Response<ApiResponse<Material>>) {
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Add Material"
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AddMaterialActivity, "PDF material uploaded successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMaterialActivity, "Failed to upload PDF material", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Add Material"
                    Toast.makeText(this@AddMaterialActivity, "Network error uploading PDF", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
