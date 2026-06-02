package com.example.researchcenter.features.material

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.BookmarkToggleResponse
import com.example.researchcenter.shared.model.Material
import com.example.researchcenter.shared.model.MaterialNote
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MaterialDetailActivity : AppCompatActivity() {

    private var materialId: Long = -1
    private var repoId: Long = -1
    private var isOwner: Boolean = false

    private lateinit var tvTitle: TextView
    private lateinit var tvMeta: TextView
    private lateinit var tvType: TextView
    private lateinit var tvStatusChip: TextView
    private lateinit var actPersonalStatus: AutoCompleteTextView
    private lateinit var btnOpenLink: Button
    private lateinit var tvAuthors: TextView
    private lateinit var tvPublisher: TextView
    private lateinit var tvYearIsbn: TextView
    private lateinit var tvDesc: TextView
    private lateinit var llTags: LinearLayout
    private lateinit var etNote: TextInputEditText
    private lateinit var btnSaveNote: MaterialButton
    private lateinit var btnBookmark: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var cardReferenceDetails: MaterialCardView

    private var currentMaterial: Material? = null
    private val statusValues = listOf("TO_READ", "IN_PROGRESS", "COMPLETED")
    private val statusLabels = mapOf(
        "TO_READ" to "To Read",
        "IN_PROGRESS" to "In Progress",
        "COMPLETED" to "Completed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_detail)

        materialId = intent.getLongExtra("MATERIAL_ID", -1L)
        repoId = intent.getLongExtra("REPO_ID", -1L)
        isOwner = intent.getBooleanExtra("IS_OWNER", false)

        if (materialId == -1L) {
            Toast.makeText(this, "Invalid Material ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadMaterialDetails()
    }

    private fun initViews() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        tvTitle = findViewById(R.id.tvMatTitle)
        tvMeta = findViewById(R.id.tvMatMeta)
        tvType = findViewById(R.id.tvMatType)
        tvStatusChip = findViewById(R.id.tvMatStatusChip)
        actPersonalStatus = findViewById(R.id.actMatPersonalStatus)
        btnOpenLink = findViewById(R.id.btnMatOpenLink)
        tvAuthors = findViewById(R.id.tvMatAuthors)
        tvPublisher = findViewById(R.id.tvMatPublisher)
        tvYearIsbn = findViewById(R.id.tvMatYearIsbn)
        tvDesc = findViewById(R.id.tvMatDesc)
        llTags = findViewById(R.id.llMatTags)
        etNote = findViewById(R.id.etMatNote)
        btnSaveNote = findViewById(R.id.btnMatSaveNote)
        btnBookmark = findViewById(R.id.btnMatBookmark)
        btnDelete = findViewById(R.id.btnMatDelete)
        cardReferenceDetails = findViewById(R.id.cardReferenceDetails)

        // Status Dropdown Setup
        actPersonalStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, statusValues.map { statusLabels[it] ?: it })
        )

        actPersonalStatus.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = statusValues[position]
            updateMaterialStatus(selectedStatus)
        }

        btnSaveNote.setOnClickListener {
            savePrivateNote()
        }
    }

    private fun loadMaterialDetails() {
        RetrofitClient.createService<MaterialApi>().getMaterial(materialId)
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(
                    call: Call<ApiResponse<Material>>,
                    response: Response<ApiResponse<Material>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { material ->
                            currentMaterial = material
                            runOnUiThread {
                                populateDetails(material)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {}
            })

        // Load personal private note
        RetrofitClient.createService<MaterialApi>().getNote(materialId)
            .enqueue(object : Callback<ApiResponse<MaterialNote>> {
                override fun onResponse(
                    call: Call<ApiResponse<MaterialNote>>,
                    response: Response<ApiResponse<MaterialNote>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val note = response.body()?.data
                        runOnUiThread {
                            etNote.setText(note?.content ?: "")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialNote>>, t: Throwable) {}
            })
    }

    private fun populateDetails(material: Material) {
        try {
            tvTitle.text = material.title ?: ""
            tvMeta.text = "Uploaded by ${material.uploaderName ?: "Unknown"}"
            tvType.text = " ${material.materialType ?: "PDF"}"
            tvDesc.text = material.description?.takeIf { it.isNotBlank() } ?: "No description provided."

            // Status Chip styling
            val statusString = material.myStatus ?: material.status ?: "TO_READ"
            applyStatusChip(tvStatusChip, statusString)
            actPersonalStatus.setText(statusLabels[statusString.uppercase()] ?: "To Read", false)

            // Tags
            llTags.removeAllViews()
            material.tags?.forEach { tag ->
                val tagView = TextView(this).apply {
                    text = tag
                    textSize = 11f
                    setTextColor(Color.parseColor("#374151"))
                    setPadding(16, 6, 16, 6)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 999f
                        setColor(Color.parseColor("#E5E7EB"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 12, 0)
                    }
                }
                llTags.addView(tagView)
            }

            // Bookmark action styling
            if (material.bookmarked) {
                btnBookmark.text = "Bookmarked"
                btnBookmark.setTextColor(Color.parseColor("#16A34A"))
                btnBookmark.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A")))
                btnBookmark.setIconResource(R.drawable.ic_bookmark)
                btnBookmark.setIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A")))
            } else {
                btnBookmark.text = "Bookmark"
                btnBookmark.setTextColor(Color.parseColor("#374151"))
                btnBookmark.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1D5DB")))
                btnBookmark.setIconResource(R.drawable.ic_bookmark_border)
                btnBookmark.setIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#9CA3AF")))
            }

            btnBookmark.setOnClickListener {
                toggleBookmark()
            }

            // Open Document action
            val fileUrl = material.fileUrl ?: material.url
            if (!fileUrl.isNullOrBlank()) {
                btnOpenLink.visibility = View.VISIBLE
                btnOpenLink.text = if (material.materialType == "PDF") "Open PDF" else "Open Link"
                btnOpenLink.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to open link", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                btnOpenLink.visibility = View.GONE
            }

            // Reference-specific values
            if (material.materialType.equals("REFERENCE", ignoreCase = true)) {
                cardReferenceDetails.visibility = View.VISIBLE
                if (!material.authors.isNullOrBlank()) {
                    tvAuthors.visibility = View.VISIBLE
                    tvAuthors.text = "Authors: ${material.authors}"
                } else {
                    tvAuthors.visibility = View.GONE
                }
                if (!material.publisher.isNullOrBlank()) {
                    tvPublisher.visibility = View.VISIBLE
                    tvPublisher.text = "Publisher: ${material.publisher}"
                } else {
                    tvPublisher.visibility = View.GONE
                }

                val yearStr = material.year?.takeIf { it.isNotBlank() } ?: ""
                val isbnStr = material.isbn?.takeIf { it.isNotBlank() } ?: ""
                if (yearStr.isNotBlank() || isbnStr.isNotBlank()) {
                    tvYearIsbn.visibility = View.VISIBLE
                    tvYearIsbn.text = "Year: $yearStr    ISBN: $isbnStr"

                    if (isbnStr.isNotBlank()) {
                        tvYearIsbn.isClickable = true
                        tvYearIsbn.setOnClickListener {
                            try {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ISBN", isbnStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(this, "ISBN Copied!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    tvYearIsbn.visibility = View.GONE
                }
            } else {
                cardReferenceDetails.visibility = View.GONE
            }

            // Delete button
            val currentUserId = SessionManager.getUserId(this)
            val userRole = SessionManager.getRole(this)
            val canDelete = userRole == "ADMIN" || material.uploaderId == currentUserId

            if (canDelete) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    confirmDeleteMaterial()
                }
            } else {
                btnDelete.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyStatusChip(view: TextView, status: String) {
        val normalized = status.uppercase()
        view.text = statusLabels[normalized] ?: normalized
        
        val (bgColor, textColor, borderColor) = when (normalized) {
            "TO_READ" -> Triple(Color.parseColor("#FEF9C3"), Color.parseColor("#A16207"), Color.parseColor("#FDE047"))
            "IN_PROGRESS" -> Triple(Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8"), Color.parseColor("#93C5FD"))
            "COMPLETED" -> Triple(Color.parseColor("#DCFCE7"), Color.parseColor("#166534"), Color.parseColor("#86EFAC"))
            else -> Triple(Color.parseColor("#F3F4F6"), Color.parseColor("#374151"), Color.parseColor("#E5E7EB"))
        }

        view.setTextColor(textColor)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
            setStroke(2, borderColor)
        }
    }

    private fun updateMaterialStatus(newStatus: String) {
        RetrofitClient.createService<MaterialApi>().updateStatus(materialId, mapOf("status" to newStatus))
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(
                    call: Call<ApiResponse<Material>>,
                    response: Response<ApiResponse<Material>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        runOnUiThread {
                            Toast.makeText(this@MaterialDetailActivity, "Status updated!", Toast.LENGTH_SHORT).show()
                            tvStatusChip.text = statusLabels[newStatus] ?: newStatus
                            applyStatusChip(tvStatusChip, newStatus)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Material>>, t: Throwable) {}
            })
    }

    private fun toggleBookmark() {
        RetrofitClient.createService<MaterialApi>().toggleBookmark(materialId)
            .enqueue(object : Callback<ApiResponse<BookmarkToggleResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<BookmarkToggleResponse>>,
                    response: Response<ApiResponse<BookmarkToggleResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val bookmarked = response.body()?.data?.bookmarked ?: false
                        runOnUiThread {
                            Toast.makeText(
                                this@MaterialDetailActivity,
                                if (bookmarked) "Bookmarked!" else "Unbookmarked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadMaterialDetails()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<BookmarkToggleResponse>>, t: Throwable) {}
            })
    }

    private fun savePrivateNote() {
        val noteContent = etNote.text.toString().trim()
        RetrofitClient.createService<MaterialApi>().saveNote(materialId, mapOf("content" to noteContent))
            .enqueue(object : Callback<ApiResponse<MaterialNote>> {
                override fun onResponse(
                    call: Call<ApiResponse<MaterialNote>>,
                    response: Response<ApiResponse<MaterialNote>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        runOnUiThread {
                            Toast.makeText(this@MaterialDetailActivity, "Note saved privately!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialNote>>, t: Throwable) {}
            })
    }

    private fun confirmDeleteMaterial() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Material")
            .setMessage("Are you sure you want to delete this material?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMaterial()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMaterial() {
        RetrofitClient.createService<MaterialApi>().deleteMaterial(materialId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@MaterialDetailActivity, "Material deleted successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
    }
}
