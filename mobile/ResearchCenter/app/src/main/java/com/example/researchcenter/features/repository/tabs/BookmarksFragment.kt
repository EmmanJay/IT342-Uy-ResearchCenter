package com.example.researchcenter.features.repository.tabs

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.features.material.EditMaterialActivity
import com.example.researchcenter.features.material.MaterialDetailBottomSheet
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.BookmarkToggleResponse
import com.example.researchcenter.shared.model.Material
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class BookmarksFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    
    private val allBookmarks = mutableListOf<Material>()
    private val displayedBookmarks = mutableListOf<Material>()
    
    private lateinit var adapter: MaterialAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMaterials: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: View
    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerSort: Spinner
    private lateinit var tvActiveCount: TextView

    private val sortOptions = listOf("Latest", "Oldest")
    private var currentSort = "Latest"
    private var searchQuery = ""

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): BookmarksFragment {
            val f = BookmarksFragment()
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
        val view = inflater.inflate(R.layout.fragment_materials, container, false)
        
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvMaterials = view.findViewById(R.id.rv_materials)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnAdd = view.findViewById(R.id.btn_add_material)
        etSearch = view.findViewById(R.id.et_search)
        spinnerSort = view.findViewById(R.id.spinner_sort)
        tvActiveCount = view.findViewById(R.id.tv_active_filters_count)

        // Bookmarks tab doesn't have an Add button
        btnAdd.visibility = View.GONE
        tvEmpty.text = "No bookmarks found"
        
        setupAdapter()
        rvMaterials.layoutManager = LinearLayoutManager(context)
        rvMaterials.adapter = adapter

        setupSortSpinner()
        setupSearchInput()

        swipeRefresh.setOnRefreshListener { loadBookmarks() }
        loadBookmarks()
        
        return view
    }

    private fun setupAdapter() {
        adapter = MaterialAdapter(
            materials = displayedBookmarks,
            isOwner = isOwner,
            onToggleBookmark = { material -> toggleBookmark(material) },
            onEdit = { material -> editMaterial(material) },
            onDelete = { material -> confirmDeleteMaterial(material) },
            onItemClick = { material -> showMaterialDetail(material) }
        )
        rvMaterials.adapter = adapter
    }

    private fun setupSortSpinner() {
        spinnerSort.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSort = sortOptions[position]
                filterAndSort()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearchInput() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().lowercase(Locale.getDefault())
                filterAndSort()
            }
        })
    }

    private fun filterAndSort() {
        var filteredList = allBookmarks.toList()

        // Apply Search
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase(Locale.getDefault()).contains(searchQuery) ||
                (it.description?.lowercase(Locale.getDefault())?.contains(searchQuery) == true) ||
                it.tags.any { tag -> tag.lowercase(Locale.getDefault()).contains(searchQuery) }
            }
        }

        // Apply Sorting
        filteredList = if (currentSort == "Latest") {
            filteredList.sortedByDescending { it.createdAt }
        } else {
            filteredList.sortedBy { it.createdAt }
        }

        displayedBookmarks.clear()
        displayedBookmarks.addAll(filteredList)
        adapter.notifyDataSetChanged()

        tvEmpty.visibility = if (displayedBookmarks.isEmpty()) View.VISIBLE else View.GONE
        tvActiveCount.text = "Showing ${displayedBookmarks.size} bookmarked materials"
    }

    private fun loadBookmarks() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().getMaterials(repoId)
            .enqueue(object : Callback<ApiResponse<List<Material>>> {
                override fun onResponse(call: Call<ApiResponse<List<Material>>>, response: Response<ApiResponse<List<Material>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            allBookmarks.clear()
                            response.body()?.data?.let { list ->
                                allBookmarks.addAll(list.filter { it.bookmarked })
                            }
                            filterAndSort()
                        } else {
                            Toast.makeText(context, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<Material>>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun toggleBookmark(material: Material) {
        RetrofitClient.createService<MaterialApi>().toggleBookmark(material.id)
            .enqueue(object : Callback<ApiResponse<BookmarkToggleResponse>> {
                override fun onResponse(call: Call<ApiResponse<BookmarkToggleResponse>>, response: Response<ApiResponse<BookmarkToggleResponse>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val bookmarked = response.body()?.data?.bookmarked ?: false
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                if (bookmarked) "Bookmarked!" else "Unbookmarked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadBookmarks()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<BookmarkToggleResponse>>, t: Throwable) {}
            })
    }

    private fun editMaterial(material: Material) {
        val intent = Intent(context, EditMaterialActivity::class.java).apply {
            putExtra("MATERIAL_ID", material.id)
            putExtra("REPO_ID", repoId)
            putExtra("REPO_NAME", activity?.intent?.getStringExtra("REPO_NAME") ?: "Repository")
        }
        startActivity(intent)
    }

    private fun confirmDeleteMaterial(material: Material) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Material")
            .setMessage("Are you sure you want to delete this material?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMaterial(material.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMaterial(materialId: Long) {
        RetrofitClient.createService<MaterialApi>().deleteMaterial(materialId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Material deleted successfully", Toast.LENGTH_SHORT).show()
                            loadBookmarks()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
    }

    private fun showMaterialDetail(material: Material) {
        MaterialDetailBottomSheet.newInstance(material.id, repoId, isOwner) {
            loadBookmarks()
        }.show(childFragmentManager, "MaterialDetail")
    }

    override fun onResume() {
        super.onResume()
        if (::swipeRefresh.isInitialized) {
            loadBookmarks()
        }
    }
}
