package com.example.researchcenter.features.bookmarks

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.features.repository.RepositoryDetailActivity
import com.example.researchcenter.shared.api.MaterialApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GlobalBookmarksFragment : Fragment() {

    private val allBookmarks = mutableListOf<Material>()
    private val filteredBookmarks = mutableListOf<Material>()
    private lateinit var adapter: GlobalBookmarkAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvBookmarks: RecyclerView
    private lateinit var layoutEmpty: View
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bookmarks_global, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvBookmarks = view.findViewById(R.id.rv_bookmarks)
        layoutEmpty = view.findViewById(R.id.layout_empty)

        adapter = GlobalBookmarkAdapter(filteredBookmarks) { material ->
            val intent = Intent(context, RepositoryDetailActivity::class.java)
            intent.putExtra("REPO_ID", material.repositoryId)
            startActivity(intent)
        }
        rvBookmarks.layoutManager = LinearLayoutManager(context)
        rvBookmarks.adapter = adapter

        swipeRefresh.setColorSchemeColors(resources.getColor(R.color.primary_green, null))
        swipeRefresh.setOnRefreshListener { loadBookmarks() }

        val etSearch = view.findViewById<TextInputEditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().lowercase()
                filterBookmarks()
            }
        })

        loadBookmarks()
    }

    private fun loadBookmarks() {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<MaterialApi>().getBookmarkedMaterials()
            .enqueue(object : Callback<ApiResponse<List<Material>>> {
                override fun onResponse(call: Call<ApiResponse<List<Material>>>, response: Response<ApiResponse<List<Material>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            allBookmarks.clear()
                            response.body()?.data?.let { allBookmarks.addAll(it) }
                            filterBookmarks()
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

    private fun filterBookmarks() {
        filteredBookmarks.clear()
        if (searchQuery.isEmpty()) {
            filteredBookmarks.addAll(allBookmarks)
        } else {
            filteredBookmarks.addAll(allBookmarks.filter {
                it.title.lowercase().contains(searchQuery) ||
                (it.description?.lowercase()?.contains(searchQuery) == true)
            })
        }
        adapter.notifyDataSetChanged()
        layoutEmpty.visibility = if (filteredBookmarks.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::swipeRefresh.isInitialized) loadBookmarks()
    }
}
