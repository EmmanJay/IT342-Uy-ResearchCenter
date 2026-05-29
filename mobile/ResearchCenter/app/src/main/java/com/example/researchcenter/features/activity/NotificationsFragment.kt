package com.example.researchcenter.features.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.features.profile.ActivityLogAdapter
import com.example.researchcenter.shared.api.ActivityApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ActivityLog
import com.example.researchcenter.shared.model.ApiResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsFragment : Fragment() {

    private val notifications = mutableListOf<ActivityLog>()
    private lateinit var adapter: ActivityLogAdapter
    private lateinit var rvNotifications: RecyclerView
    private lateinit var btnLoadMore: MaterialButton
    private lateinit var layoutEmpty: View
    private lateinit var progressLoading: ProgressBar
    private var currentPage = 0
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvNotifications = view.findViewById(R.id.rv_notifications)
        btnLoadMore = view.findViewById(R.id.btn_load_more)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        progressLoading = view.findViewById(R.id.progress_loading)

        adapter = ActivityLogAdapter(notifications)
        rvNotifications.layoutManager = LinearLayoutManager(context)
        rvNotifications.adapter = adapter

        btnLoadMore.setOnClickListener {
            currentPage++
            loadNotifications(append = true)
        }

        loadNotifications(append = false)
    }

    private fun loadNotifications(append: Boolean) {
        if (isLoading) return
        isLoading = true

        if (!append) {
            progressLoading.visibility = View.VISIBLE
            currentPage = 0
        }

        RetrofitClient.createService<ActivityApi>().getNotifications(currentPage, 10)
            .enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
                override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                    activity?.runOnUiThread {
                        isLoading = false
                        progressLoading.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.success == true) {
                            val data = response.body()?.data ?: emptyList()
                            if (!append) notifications.clear()
                            notifications.addAll(data)
                            adapter.notifyDataSetChanged()

                            layoutEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                            rvNotifications.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE
                            btnLoadMore.visibility = if (data.size >= 10) View.VISIBLE else View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                    activity?.runOnUiThread {
                        isLoading = false
                        progressLoading.visibility = View.GONE
                        Toast.makeText(context, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (::rvNotifications.isInitialized) {
            currentPage = 0
            loadNotifications(append = false)
        }
    }
}
