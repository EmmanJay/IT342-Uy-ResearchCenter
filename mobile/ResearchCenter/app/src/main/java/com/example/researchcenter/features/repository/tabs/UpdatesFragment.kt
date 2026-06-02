package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.RepositoryUpdate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdatesFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val updates = mutableListOf<RepositoryUpdate>()
    private lateinit var adapter: UpdateAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvUpdates: RecyclerView
    private lateinit var tvEmpty: View
    
    // Post update section UI elements
    private lateinit var llPostUpdate: View
    private lateinit var etNewUpdate: TextInputEditText
    private lateinit var btnPostUpdate: View

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): UpdatesFragment {
            val f = UpdatesFragment()
            val args = Bundle()
            args.putLong("repoId", repoId)
            args.putBoolean("isOwner", isOwner)
            f.arguments = args
            return f
        }
    }
    
    fun setIsOwner(owner: Boolean) {
        this.isOwner = owner
        if (::adapter.isInitialized) {
            setupAdapter()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repoId = arguments?.getLong("repoId") ?: -1
        isOwner = arguments?.getBoolean("isOwner") ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_updates, container, false)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvUpdates = view.findViewById(R.id.rv_updates)
        tvEmpty = view.findViewById(R.id.ll_empty_state)
        
        llPostUpdate = view.findViewById(R.id.ll_post_update)
        etNewUpdate = view.findViewById(R.id.et_new_update)
        btnPostUpdate = view.findViewById(R.id.btn_post_update)
        
        // Show write section for anyone who can view this repository
        llPostUpdate.visibility = View.VISIBLE
        
        btnPostUpdate.setOnClickListener {
            val content = etNewUpdate.text.toString().trim()
            if (content.isNotEmpty()) {
                postUpdate(content)
            } else {
                Toast.makeText(context, "Please enter some update content", Toast.LENGTH_SHORT).show()
            }
        }
        
        setupAdapter()
        rvUpdates.layoutManager = LinearLayoutManager(context)

        swipeRefresh.setOnRefreshListener { loadUpdates() }
        loadUpdates()
        
        return view
    }

    private fun setupAdapter() {
        val currentUserId = SessionManager.getUserId(requireContext())
        adapter = UpdateAdapter(
            updates = updates,
            isOwner = isOwner,
            currentUserId = currentUserId,
            onEdit = { update -> showEditUpdateDialog(update) },
            onDelete = { update -> confirmDeleteUpdate(update) }
        )
        rvUpdates.adapter = adapter
    }

    private fun postUpdate(content: String) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().createUpdate(repoId, mapOf("content" to content))
            .enqueue(object : Callback<ApiResponse<RepositoryUpdate>> {
                override fun onResponse(call: Call<ApiResponse<RepositoryUpdate>>, response: Response<ApiResponse<RepositoryUpdate>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Update posted successfully!", Toast.LENGTH_SHORT).show()
                            etNewUpdate.text?.clear()
                            loadUpdates()
                        } else {
                            val msg = response.body()?.error?.message ?: "Failed to post update"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<RepositoryUpdate>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun showEditUpdateDialog(update: RepositoryUpdate) {
        val input = EditText(context)
        input.setText(update.content)
        input.setPadding(48, 48, 48, 48)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Update")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    editUpdate(update.id, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editUpdate(updateId: Long, newContent: String) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().editUpdate(repoId, updateId, mapOf("content" to newContent))
            .enqueue(object : Callback<ApiResponse<RepositoryUpdate>> {
                override fun onResponse(call: Call<ApiResponse<RepositoryUpdate>>, response: Response<ApiResponse<RepositoryUpdate>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Update edited successfully!", Toast.LENGTH_SHORT).show()
                            loadUpdates()
                        } else {
                            val msg = response.body()?.error?.message ?: "Failed to edit update"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<RepositoryUpdate>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun confirmDeleteUpdate(update: RepositoryUpdate) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Update")
            .setMessage("Are you sure you want to delete this update?")
            .setPositiveButton("Delete") { _, _ ->
                deleteUpdate(update.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUpdate(updateId: Long) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().deleteUpdate(repoId, updateId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Update deleted successfully", Toast.LENGTH_SHORT).show()
                            loadUpdates()
                        } else {
                            Toast.makeText(context, "Failed to delete update", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun loadUpdates() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().getUpdates(repoId)
            .enqueue(object : Callback<ApiResponse<List<RepositoryUpdate>>> {
                override fun onResponse(call: Call<ApiResponse<List<RepositoryUpdate>>>, response: Response<ApiResponse<List<RepositoryUpdate>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            updates.clear()
                            response.body()?.data?.let { updates.addAll(it) }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (updates.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<RepositoryUpdate>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }
}
