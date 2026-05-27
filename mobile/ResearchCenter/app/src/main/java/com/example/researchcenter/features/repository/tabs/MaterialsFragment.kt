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

class MaterialsFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val materials = mutableListOf<Material>()
    private lateinit var adapter: MaterialAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMaterials: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: View

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
        if (::btnAdd.isInitialized) {
            btnAdd.visibility = if (isOwner) View.VISIBLE else View.GONE
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

        btnAdd.visibility = if (isOwner) View.VISIBLE else View.GONE
        
        adapter = MaterialAdapter(materials) { material ->
            toggleBookmark(material)
        }
        rvMaterials.layoutManager = LinearLayoutManager(context)
        rvMaterials.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadMaterials() }
        loadMaterials()
        
        return view
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
                            materials.clear()
                            response.body()?.data?.let { materials.addAll(it) }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (materials.isEmpty()) View.VISIBLE else View.GONE
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
        // Implement toggle logic if needed in mobile, or simply re-fetch
    }
}
