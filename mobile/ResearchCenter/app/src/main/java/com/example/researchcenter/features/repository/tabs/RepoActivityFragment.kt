package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.features.profile.ActivityLogAdapter
import com.example.researchcenter.shared.api.ActivityApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ActivityLog
import com.example.researchcenter.shared.model.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepoActivityFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val activities = mutableListOf<ActivityLog>()
    private lateinit var adapter: ActivityLogAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvActivities: RecyclerView
    private lateinit var tvEmpty: TextView

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): RepoActivityFragment {
            val f = RepoActivityFragment()
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
        val view = inflater.inflate(R.layout.fragment_repo_activity, container, false)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvActivities = view.findViewById(R.id.rv_activities)
        tvEmpty = view.findViewById(R.id.tv_empty)
        
        adapter = ActivityLogAdapter(activities)
        rvActivities.layoutManager = LinearLayoutManager(context)
        rvActivities.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadActivities() }
        loadActivities()
        
        return view
    }

    private fun loadActivities() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<ActivityApi>().getRepositoryActivities(repoId)
            .enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
                override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            activities.clear()
                            response.body()?.data?.let { activities.addAll(it) }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (activities.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }
}
