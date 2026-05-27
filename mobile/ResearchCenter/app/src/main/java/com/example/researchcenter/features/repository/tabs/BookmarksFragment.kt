package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Material
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookmarksFragment : Fragment() {
    private var repoId: Long = -1
    private val materials = mutableListOf<Material>()
    private lateinit var adapter: MaterialAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMaterials: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: View

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): BookmarksFragment {
            val f = BookmarksFragment()
            val args = Bundle()
            args.putLong("repoId", repoId)
            f.arguments = args
            return f
        }
    }
    
    fun setIsOwner(owner: Boolean) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repoId = arguments?.getLong("repoId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_materials, container, false)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvMaterials = view.findViewById(R.id.rv_materials)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnAdd = view.findViewById(R.id.btn_add_material)
        
        btnAdd.visibility = View.GONE
        tvEmpty.text = "No bookmarks found"
        
        adapter = MaterialAdapter(materials) {}
        rvMaterials.layoutManager = LinearLayoutManager(context)
        rvMaterials.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadBookmarks() }
        loadBookmarks()
        
        return view
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
                            materials.clear()
                            response.body()?.data?.let { list -> 
                                materials.addAll(list.filter { it.bookmarked }) 
                            }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (materials.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<Material>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }
}
