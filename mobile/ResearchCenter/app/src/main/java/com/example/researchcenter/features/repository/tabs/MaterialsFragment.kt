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
import com.example.researchcenter.features.material.AddMaterialActivity
import com.example.researchcenter.features.material.EditMaterialActivity
import com.example.researchcenter.features.material.MaterialDetailActivity
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

class MaterialsFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    
    private val allMaterials = mutableListOf<Material>()
    private val displayedMaterials = mutableListOf<Material>()
    
    private lateinit var adapter: MaterialAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMaterials: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerSort: Spinner
    private lateinit var tvActiveCount: TextView

    private val sortOptions = listOf("Latest", "Oldest")
    private var currentSort = "Latest"
    private var searchQuery = ""
    private val selectedStatuses = mutableListOf<String>()
    private val selectedTypes = mutableListOf<String>()
    private val activeTags = mutableListOf<String>()

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): MaterialsFragment {
            val f = MaterialsFragment()
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
            // Re-create adapter or notify
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
        etSearch = view.findViewById(R.id.et_search)
        spinnerSort = view.findViewById(R.id.spinner_sort)
        tvActiveCount = view.findViewById(R.id.tv_active_filters_count)

        setupAdapter()
        rvMaterials.layoutManager = LinearLayoutManager(context)
        rvMaterials.adapter = adapter

        setupSortSpinner()
        setupSearchInput()



        val btnFilters: View = view.findViewById(R.id.btn_filters)
        val btnTags: View = view.findViewById(R.id.btn_tags)
        btnFilters.setOnClickListener { showFiltersDialog() }
        btnTags.setOnClickListener { showTagsDialog() }

        swipeRefresh.setOnRefreshListener { loadMaterials() }
        loadMaterials()
        
        return view
    }

    private fun setupAdapter() {
        adapter = MaterialAdapter(
            materials = displayedMaterials,
            isOwner = isOwner,
            onToggleBookmark = { material -> toggleBookmark(material) },
            onEdit = { material -> editMaterial(material) },
            onDelete = { material -> confirmDeleteMaterial(material) },
            onItemClick = { material -> showMaterialDetail(material) }
        )
        rvMaterials.adapter = adapter
    }

    private fun setupSortSpinner() {
        val sortAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, sortOptions)
        sortAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerSort.adapter = sortAdapter
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
        var filteredList = allMaterials.toList()

        // Apply Search
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase(Locale.getDefault()).contains(searchQuery) ||
                (it.description?.lowercase(Locale.getDefault())?.contains(searchQuery) == true) ||
                it.tags.any { tag -> tag.lowercase(Locale.getDefault()).contains(searchQuery) }
            }
        }

        // Apply Status Filter
        if (selectedStatuses.isNotEmpty()) {
            filteredList = filteredList.filter {
                selectedStatuses.contains(it.myStatus ?: it.status ?: "TO_READ")
            }
        }

        // Apply Material Type Filter
        if (selectedTypes.isNotEmpty()) {
            filteredList = filteredList.filter {
                selectedTypes.contains(it.materialType)
            }
        }

        // Apply Tags Filter
        if (activeTags.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.tags.any { tag -> activeTags.contains(tag) }
            }
        }

        // Apply Sorting
        filteredList = if (currentSort == "Latest") {
            filteredList.sortedByDescending { it.createdAt }
        } else {
            filteredList.sortedBy { it.createdAt }
        }

        displayedMaterials.clear()
        displayedMaterials.addAll(filteredList)
        adapter.notifyDataSetChanged()

        tvEmpty.visibility = if (displayedMaterials.isEmpty()) View.VISIBLE else View.GONE
        
        var activeFilterCount = selectedStatuses.size + selectedTypes.size + activeTags.size
        var infoText = "Showing ${displayedMaterials.size} materials"
        if (activeFilterCount > 0) {
            infoText += " ($activeFilterCount filters active)"
        }
        tvActiveCount.text = infoText
    }

    private fun showFiltersDialog() {
        val filterOptions = arrayOf("To Read", "In Progress", "Completed", "PDF", "Link")
        val checkedItems = BooleanArray(filterOptions.size) { index ->
            when (index) {
                0 -> selectedStatuses.contains("TO_READ")
                1 -> selectedStatuses.contains("IN_PROGRESS")
                2 -> selectedStatuses.contains("COMPLETED")
                3 -> selectedTypes.contains("PDF")
                4 -> selectedTypes.contains("LINK")
                else -> false
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Filters")
            .setMultiChoiceItems(filterOptions, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                selectedStatuses.clear()
                selectedTypes.clear()
                if (checkedItems[0]) selectedStatuses.add("TO_READ")
                if (checkedItems[1]) selectedStatuses.add("IN_PROGRESS")
                if (checkedItems[2]) selectedStatuses.add("COMPLETED")
                if (checkedItems[3]) selectedTypes.add("PDF")
                if (checkedItems[4]) selectedTypes.add("LINK")
                filterAndSort()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTagsDialog() {
        val distinctTags = allMaterials.flatMap { it.tags }.distinct().sorted()
        if (distinctTags.isEmpty()) {
            Toast.makeText(context, "No tags available", Toast.LENGTH_SHORT).show()
            return
        }

        val tagArray = distinctTags.toTypedArray()
        val checkedItems = BooleanArray(tagArray.size) { index ->
            activeTags.contains(tagArray[index])
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Tags")
            .setMultiChoiceItems(tagArray, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                activeTags.clear()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        activeTags.add(tagArray[i])
                    }
                }
                filterAndSort()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadMaterials() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().getMaterials(repoId)
            .enqueue(object : Callback<ApiResponse<List<Material>>> {
                override fun onResponse(call: Call<ApiResponse<List<Material>>>, response: Response<ApiResponse<List<Material>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            allMaterials.clear()
                            response.body()?.data?.let { allMaterials.addAll(it) }
                            filterAndSort()
                        } else {
                            Toast.makeText(context, "Failed to load materials", Toast.LENGTH_SHORT).show()
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
                        val updatedBookmarked = response.body()?.data?.bookmarked ?: !material.bookmarked
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                if (updatedBookmarked) "Bookmarked!" else "Unbookmarked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadMaterials()
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
                            loadMaterials()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
    }

    private fun showMaterialDetail(material: Material) {
        com.example.researchcenter.features.material.MaterialDetailBottomSheet.newInstance(
            material.id, repoId, isOwner
        ) {
            loadMaterials()
        }.show(childFragmentManager, "MaterialDetail")
    }

    override fun onResume() {
        super.onResume()
        if (::swipeRefresh.isInitialized) {
            loadMaterials()
        }
    }
}
