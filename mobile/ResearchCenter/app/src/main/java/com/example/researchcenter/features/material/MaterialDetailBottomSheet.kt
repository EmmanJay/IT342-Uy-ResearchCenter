package com.example.researchcenter.features.material

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.BookmarkToggleResponse
import com.example.researchcenter.shared.model.Material
import com.example.researchcenter.shared.model.MaterialNote
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MaterialDetailBottomSheet : BottomSheetDialogFragment() {

    private var materialId: Long = -1
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private var onRefresh: (() -> Unit)? = null

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

    private var currentMaterial: Material? = null
    private val statusValues = listOf("TO_READ", "IN_PROGRESS", "COMPLETED")
    private val statusLabels = mapOf(
        "TO_READ" to "To Read",
        "IN_PROGRESS" to "In Progress",
        "COMPLETED" to "Completed"
    )

    companion object {
        fun newInstance(
            materialId: Long,
            repoId: Long,
            isOwner: Boolean,
            onRefresh: () -> Unit
        ): MaterialDetailBottomSheet {
            val sheet = MaterialDetailBottomSheet()
            sheet.materialId = materialId
            sheet.repoId = repoId
            sheet.isOwner = isOwner
            sheet.onRefresh = onRefresh
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_material_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadMaterialDetails()
    }

    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tvMatTitle)
        tvMeta = view.findViewById(R.id.tvMatMeta)
        tvType = view.findViewById(R.id.tvMatType)
        tvStatusChip = view.findViewById(R.id.tvMatStatusChip)
        actPersonalStatus = view.findViewById(R.id.actMatPersonalStatus)
        btnOpenLink = view.findViewById(R.id.btnMatOpenLink)
        tvAuthors = view.findViewById(R.id.tvMatAuthors)
        tvPublisher = view.findViewById(R.id.tvMatPublisher)
        tvYearIsbn = view.findViewById(R.id.tvMatYearIsbn)
        tvDesc = view.findViewById(R.id.tvMatDesc)
        llTags = view.findViewById(R.id.llMatTags)
        etNote = view.findViewById(R.id.etMatNote)
        btnSaveNote = view.findViewById(R.id.btnMatSaveNote)
        btnBookmark = view.findViewById(R.id.btnMatBookmark)
        btnDelete = view.findViewById(R.id.btnMatDelete)

        // Status Dropdown Setup
        actPersonalStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusValues.map { statusLabels[it] ?: it })
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
        if (materialId == -1L) return

        RetrofitClient.createService<MaterialApi>().getMaterial(materialId)
            .enqueue(object : Callback<ApiResponse<Material>> {
                override fun onResponse(
                    call: Call<ApiResponse<Material>>,
                    response: Response<ApiResponse<Material>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { material ->
                            currentMaterial = material
                            activity?.runOnUiThread {
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
                        activity?.runOnUiThread {
                            etNote.setText(note?.content ?: "")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialNote>>, t: Throwable) {}
            })
    }

    private fun populateDetails(material: Material) {
        tvTitle.text = material.title
        tvMeta.text = "Uploaded by ${material.uploaderName}"
        tvType.text = " ${material.materialType}"
        tvDesc.text = material.description?.takeIf { it.isNotBlank() } ?: "No description provided."

        // Status Chip styling
        applyStatusChip(tvStatusChip, material.myStatus ?: material.status)
        actPersonalStatus.setText(statusLabels[material.myStatus ?: material.status] ?: "To Read", false)

        // Tags
        llTags.removeAllViews()
        material.tags.forEach { tag ->
            val tagView = TextView(requireContext()).apply {
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
            btnBookmark.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A"))
            btnBookmark.setIconResource(android.R.drawable.btn_star_big_on)
            btnBookmark.iconTint = android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A"))
        } else {
            btnBookmark.text = "Bookmark"
            btnBookmark.setTextColor(Color.parseColor("#374151"))
            btnBookmark.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            btnBookmark.setIconResource(android.R.drawable.btn_star_big_off)
            btnBookmark.iconTint = android.content.res.ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                startActivity(intent)
                dismiss()
            }
        } else {
            btnOpenLink.visibility = View.GONE
        }

        // Reference-specific values
        if (material.materialType.equals("REFERENCE", ignoreCase = true)) {
            if (!material.authors.isNullOrBlank()) {
                tvAuthors.visibility = View.VISIBLE
                tvAuthors.text = "Authors: ${material.authors}"
            }
            if (!material.publisher.isNullOrBlank()) {
                tvPublisher.visibility = View.VISIBLE
                tvPublisher.text = "Publisher: ${material.publisher}"
            }

            val yearStr = material.year?.takeIf { it.isNotBlank() } ?: ""
            val isbnStr = material.isbn?.takeIf { it.isNotBlank() } ?: ""
            if (yearStr.isNotBlank() || isbnStr.isNotBlank()) {
                tvYearIsbn.visibility = View.VISIBLE
                tvYearIsbn.text = "Year: $yearStr    ISBN: $isbnStr"

                if (isbnStr.isNotBlank()) {
                    tvYearIsbn.isClickable = true
                    tvYearIsbn.setOnClickListener {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ISBN", isbnStr)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "ISBN Copied!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Delete button
        val currentUserId = SessionManager.getUserId(requireContext())
        if (isOwner || material.uploaderId == currentUserId) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                confirmDeleteMaterial()
            }
        } else {
            btnDelete.visibility = View.GONE
        }
    }

    private fun applyStatusChip(view: TextView, status: String) {
        val normalized = status.uppercase()
        view.text = statusLabels[normalized] ?: normalized
        
        val (bgColor, textColor) = when (normalized) {
            "TO_READ" -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151")
            "IN_PROGRESS" -> Color.parseColor("#FEF3C7") to Color.parseColor("#B45309")
            "COMPLETED" -> Color.parseColor("#DCFCE7") to Color.parseColor("#15803D")
            else -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151")
        }

        view.setTextColor(textColor)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
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
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                            tvStatusChip.text = statusLabels[newStatus] ?: newStatus
                            applyStatusChip(tvStatusChip, newStatus)
                            onRefresh?.invoke()
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
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                if (bookmarked) "Bookmarked!" else "Unbookmarked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadMaterialDetails()
                            onRefresh?.invoke()
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
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Note saved privately!", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialNote>>, t: Throwable) {}
            })
    }

    private fun confirmDeleteMaterial() {
        MaterialAlertDialogBuilder(requireContext())
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
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Material deleted successfully", Toast.LENGTH_SHORT).show()
                            dismiss()
                            onRefresh?.invoke()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
    }
}
