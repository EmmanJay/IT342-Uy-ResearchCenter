package com.example.researchcenter.features.request

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RequestApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import com.example.researchcenter.shared.model.MaterialRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class RequestDetailBottomSheet : BottomSheetDialogFragment() {

    private var requestId: Long = -1
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private var onRefresh: (() -> Unit)? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvClosureNote: TextView
    private lateinit var btnCloseRequest: MaterialButton
    private lateinit var btnFulfillRequest: MaterialButton

    private var currentRequest: MaterialRequest? = null

    companion object {
        fun newInstance(
            requestId: Long,
            repoId: Long,
            isOwner: Boolean,
            onRefresh: () -> Unit
        ): RequestDetailBottomSheet {
            val sheet = RequestDetailBottomSheet()
            sheet.requestId = requestId
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
        return inflater.inflate(R.layout.bottom_sheet_request_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tv_title)
        tvSubtitle = view.findViewById(R.id.tv_subtitle)
        tvStatusBadge = view.findViewById(R.id.tv_status_badge)
        tvDescription = view.findViewById(R.id.tv_description)
        tvClosureNote = view.findViewById(R.id.tv_closure_note)
        btnCloseRequest = view.findViewById(R.id.btn_close_request)
        btnFulfillRequest = view.findViewById(R.id.btn_fulfill_request)

        loadRequestDetails()
    }

    private fun loadRequestDetails() {
        if (repoId == -1L || requestId == -1L) return

        // Load requests from repository and find the specific one to get updated data
        RetrofitClient.createService<RepositoryApi>().getRequests(repoId)
            .enqueue(object : Callback<ApiResponse<List<MaterialRequest>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<MaterialRequest>>>,
                    response: Response<ApiResponse<List<MaterialRequest>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val req = response.body()?.data?.find { it.id == requestId }
                        if (req != null) {
                            currentRequest = req
                            activity?.runOnUiThread {
                                populateDetails(req)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<MaterialRequest>>>, t: Throwable) {}
            })
    }

    private fun populateDetails(request: MaterialRequest) {
        tvTitle.text = request.title
        tvDescription.text = request.description?.takeIf { it.isNotBlank() } ?: "No description provided."

        // Format Date
        var formattedDate = request.createdAt
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formattedDate = parser.parse(request.createdAt)?.let { formatter.format(it) } ?: request.createdAt
        } catch (_: Exception) {}

        tvSubtitle.text = "Requested by ${request.requesterName} • $formattedDate"

        // Status Badge Style
        applyStatusBadge(tvStatusBadge, request.status)

        val currentUserId = SessionManager.getUserId(requireContext())
        val isRequester = request.requesterId == currentUserId

        // Closure note display
        if (request.status == "FULFILLED") {
            tvClosureNote.visibility = View.VISIBLE
            val fulfiller = request.fulfilledByName ?: "Someone"
            val matTitle = request.materialTitle ?: "attached material"
            tvClosureNote.text = "Fulfilled by $fulfiller: Material \"$matTitle\" has been attached."
            
            // Attached material click action (navigates if uploader/members want to see)
            tvClosureNote.isClickable = true
            tvClosureNote.setOnClickListener {
                request.materialId?.let { matId ->
                    Toast.makeText(context, "Opening attached material details...", Toast.LENGTH_SHORT).show()
                    // Open the material detail bottom sheet directly
                    com.example.researchcenter.features.material.MaterialDetailBottomSheet.newInstance(
                        matId, repoId, isOwner, { loadRequestDetails() }
                    ).show(parentFragmentManager, "MaterialDetail")
                }
            }
        } else {
            tvClosureNote.visibility = View.GONE
        }

        // Fulfill Button behavior
        if (request.status == "OPEN" && !isRequester) {
            btnFulfillRequest.visibility = View.VISIBLE
            btnFulfillRequest.setOnClickListener {
                showFulfillPicker()
            }
        } else {
            btnFulfillRequest.visibility = View.GONE
        }

        // Close / Delete request behavior
        if (isOwner || isRequester) {
            btnCloseRequest.visibility = View.VISIBLE
            btnCloseRequest.text = "Delete Request"
            btnCloseRequest.setTextColor(Color.parseColor("#DC2626"))
            btnCloseRequest.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCA5A5"))
            btnCloseRequest.setOnClickListener {
                confirmDeleteRequest()
            }
        } else {
            btnCloseRequest.visibility = View.GONE
        }
    }

    private fun applyStatusBadge(view: TextView, status: String) {
        val normalized = status.uppercase()
        view.text = normalized

        val (bgColor, textColor) = when (normalized) {
            "OPEN" -> Color.parseColor("#DBEAFE") to Color.parseColor("#1D4ED8")
            "FULFILLED" -> Color.parseColor("#DCFCE7") to Color.parseColor("#166534")
            "PENDING" -> Color.parseColor("#FEF3C7") to Color.parseColor("#B45309")
            "CANCELLED" -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151")
            else -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151")
        }

        view.setTextColor(textColor)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
        }
    }

    private fun showFulfillPicker() {
        // Fetch materials from repository
        RetrofitClient.createService<RepositoryApi>().getMaterials(repoId)
            .enqueue(object : Callback<ApiResponse<List<Material>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Material>>>,
                    response: Response<ApiResponse<List<Material>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val materials = response.body()?.data ?: emptyList()
                        activity?.runOnUiThread {
                            if (materials.isEmpty()) {
                                Toast.makeText(context, "No materials in repository to fulfill with. Please upload one first.", Toast.LENGTH_LONG).show()
                            } else {
                                showMaterialsSelectionDialog(materials)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Material>>>, t: Throwable) {
                    Toast.makeText(context, "Network error fetching materials.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showMaterialsSelectionDialog(materials: List<Material>) {
        val titles = materials.map { "${it.title} (${it.materialType})" }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Fulfill with Repository Material")
            .setItems(titles) { _, which ->
                val selectedMaterial = materials[which]
                fulfillRequest(selectedMaterial.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fulfillRequest(materialId: Long) {
        val requestApi = RetrofitClient.createService<RequestApi>()
        requestApi.fulfillRequest(requestId, mapOf("materialId" to materialId))
            .enqueue(object : Callback<ApiResponse<MaterialRequest>> {
                override fun onResponse(
                    call: Call<ApiResponse<MaterialRequest>>,
                    response: Response<ApiResponse<MaterialRequest>>
                ) {
                    activity?.runOnUiThread {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Request successfully fulfilled!", Toast.LENGTH_SHORT).show()
                            dismiss()
                            onRefresh?.invoke()
                        } else {
                            val errorMsg = response.body()?.error?.message ?: "Failed to fulfill request."
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MaterialRequest>>, t: Throwable) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Network error fulfilling request.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun confirmDeleteRequest() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Request")
            .setMessage("Are you sure you want to permanently delete this request?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRequest()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRequest() {
        val requestApi = RetrofitClient.createService<RequestApi>()
        requestApi.deleteRequest(requestId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Request deleted.", Toast.LENGTH_SHORT).show()
                        dismiss()
                        onRefresh?.invoke()
                    } else {
                        Toast.makeText(context, "Failed to delete request.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Network error deleting request.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
