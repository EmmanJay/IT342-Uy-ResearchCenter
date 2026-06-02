package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.features.request.NewRequestActivity
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.MaterialRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class RequestsFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val allRequests = mutableListOf<MaterialRequest>()
    private val displayedRequests = mutableListOf<MaterialRequest>()
    private lateinit var adapter: RequestAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: TextInputEditText
    
    private lateinit var spinnerSort: Spinner
    private lateinit var tvActiveCount: TextView

    private val sortOptions = listOf("Latest", "Oldest")
    private var currentSort = "Latest"
    private var searchQuery = ""
    private val selectedStatuses = mutableListOf<String>()

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): RequestsFragment {
            val f = RequestsFragment()
            val args = Bundle()
            args.putLong("repoId", repoId)
            args.putBoolean("isOwner", isOwner)
            f.arguments = args
            return f
        }
    }
    
    fun setIsOwner(owner: Boolean) {
        this.isOwner = owner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repoId = arguments?.getLong("repoId") ?: -1
        isOwner = arguments?.getBoolean("isOwner") ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_requests, container, false)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvRequests = view.findViewById(R.id.rv_requests)
        tvEmpty = view.findViewById(R.id.tv_empty)
        etSearch = view.findViewById(R.id.et_search)
        spinnerSort = view.findViewById(R.id.spinner_sort)
        tvActiveCount = view.findViewById(R.id.tv_active_filters_count)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().lowercase(Locale.getDefault())
                filterRequests()
            }
        })

        val btnFilters: View = view.findViewById(R.id.btn_filters)
        btnFilters.setOnClickListener { showFiltersDialog() }
        
        adapter = RequestAdapter(displayedRequests) { request ->
            showRequestDetail(request)
        }
        rvRequests.layoutManager = LinearLayoutManager(context)
        rvRequests.adapter = adapter

        setupSortSpinner()

        swipeRefresh.setOnRefreshListener { loadRequests() }
        loadRequests()
        
        return view
    }

    private fun setupSortSpinner() {
        val sortAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, sortOptions)
        sortAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerSort.adapter = sortAdapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSort = sortOptions[position]
                filterRequests()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showFiltersDialog() {
        val filterOptions = arrayOf("Open", "Fulfilled", "Closed", "Cancelled")
        val checkedItems = BooleanArray(filterOptions.size) { index ->
            when (index) {
                0 -> selectedStatuses.contains("OPEN")
                1 -> selectedStatuses.contains("FULFILLED")
                2 -> selectedStatuses.contains("CLOSED")
                3 -> selectedStatuses.contains("CANCELLED")
                else -> false
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Status Filters")
            .setMultiChoiceItems(filterOptions, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                selectedStatuses.clear()
                if (checkedItems[0]) selectedStatuses.add("OPEN")
                if (checkedItems[1]) selectedStatuses.add("FULFILLED")
                if (checkedItems[2]) selectedStatuses.add("CLOSED")
                if (checkedItems[3]) selectedStatuses.add("CANCELLED")
                filterRequests()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterRequests() {
        var filteredList = allRequests.toList()

        // Apply Search
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase(Locale.getDefault()).contains(searchQuery) ||
                (it.description?.lowercase(Locale.getDefault())?.contains(searchQuery) == true) ||
                it.requesterName.lowercase(Locale.getDefault()).contains(searchQuery)
            }
        }

        // Apply Status Filter
        if (selectedStatuses.isNotEmpty()) {
            filteredList = filteredList.filter {
                selectedStatuses.contains(it.status.uppercase(Locale.getDefault()))
            }
        }

        // Apply Sorting
        filteredList = if (currentSort == "Latest") {
            filteredList.sortedByDescending { it.createdAt }
        } else {
            filteredList.sortedBy { it.createdAt }
        }

        displayedRequests.clear()
        displayedRequests.addAll(filteredList)
        adapter.notifyDataSetChanged()

        tvEmpty.visibility = if (displayedRequests.isEmpty()) View.VISIBLE else View.GONE
        
        var activeFilterCount = selectedStatuses.size
        var infoText = "Showing ${displayedRequests.size} requests"
        if (activeFilterCount > 0) {
            infoText += " ($activeFilterCount filters active)"
        }
        tvActiveCount.text = infoText
    }

    private fun showRequestDetail(request: MaterialRequest) {
        com.example.researchcenter.features.request.RequestDetailBottomSheet.newInstance(
            request.id, repoId, isOwner
        ) {
            loadRequests()
        }.show(childFragmentManager, "RequestDetail")
    }

    private fun loadRequests() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().getRequests(repoId)
            .enqueue(object : Callback<ApiResponse<List<MaterialRequest>>> {
                override fun onResponse(call: Call<ApiResponse<List<MaterialRequest>>>, response: Response<ApiResponse<List<MaterialRequest>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            allRequests.clear()
                            response.body()?.data?.let { allRequests.addAll(it) }
                            filterRequests()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<MaterialRequest>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (::swipeRefresh.isInitialized) {
            loadRequests()
        }
    }
}
