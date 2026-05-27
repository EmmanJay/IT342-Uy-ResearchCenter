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
import com.example.researchcenter.shared.model.RepositoryUpdate
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
    private lateinit var tvEmpty: TextView

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
        tvEmpty = view.findViewById(R.id.tv_empty)
        
        adapter = UpdateAdapter(updates)
        rvUpdates.layoutManager = LinearLayoutManager(context)
        rvUpdates.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadUpdates() }
        loadUpdates()
        
        return view
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
