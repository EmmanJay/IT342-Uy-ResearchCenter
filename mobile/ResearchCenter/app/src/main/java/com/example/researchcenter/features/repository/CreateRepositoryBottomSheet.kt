package com.example.researchcenter.features.repository

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Repository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateRepositoryBottomSheet(
    private val onCreated: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_repository, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<TextInputEditText>(R.id.et_repo_name)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_repo_desc)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        btnCancel?.setOnClickListener { dismiss() }
        btnCreate?.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createRepository(name, desc)
        }
    }

    private fun createRepository(name: String, desc: String) {
        RetrofitClient.createService<RepositoryApi>()
            .createRepository(mapOf("name" to name, "description" to desc))
            .enqueue(object : Callback<ApiResponse<Repository>> {
                override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                    activity?.runOnUiThread {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Repository created", Toast.LENGTH_SHORT).show()
                            onCreated?.invoke()
                            dismiss()
                        } else {
                            Toast.makeText(context, "Failed to create repository", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }
}
