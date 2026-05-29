package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.content.Intent
import android.widget.TextView
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequestsFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val requests = mutableListOf<MaterialRequest>()
    private lateinit var adapter: RequestAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: View

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
        btnAdd = view.findViewById(R.id.btn_add_request)

        btnAdd.setOnClickListener {
            val intent = Intent(context, NewRequestActivity::class.java)
            intent.putExtra("REPO_ID", repoId)
            startActivity(intent)
        }
        
        adapter = RequestAdapter(requests) { request ->
            showRequestDetail(request)
        }
        rvRequests.layoutManager = LinearLayoutManager(context)
        rvRequests.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadRequests() }
        loadRequests()
        
        return view
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
                            requests.clear()
                            response.body()?.data?.let { requests.addAll(it) }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<MaterialRequest>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }
}
