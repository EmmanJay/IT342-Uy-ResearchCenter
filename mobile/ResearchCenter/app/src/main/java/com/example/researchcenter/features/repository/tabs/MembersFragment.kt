package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.RepositoryMember
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MembersFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val members = mutableListOf<RepositoryMember>()
    private lateinit var adapter: MemberAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMembers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: View

    companion object {
        fun newInstance(repoId: Long, isOwner: Boolean): MembersFragment {
            val f = MembersFragment()
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
        val view = inflater.inflate(R.layout.fragment_members, container, false)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvMembers = view.findViewById(R.id.rv_members)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnAdd = view.findViewById(R.id.btn_invite_member)

        btnAdd.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnAdd.setOnClickListener { showInviteDialog() }
        
        adapter = MemberAdapter(members)
        rvMembers.layoutManager = LinearLayoutManager(context)
        rvMembers.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadMembers() }
        loadMembers()
        
        return view
    }

    private fun showInviteDialog() {
        val input = EditText(context)
        input.hint = "Enter email address"
        input.setPadding(48, 48, 48, 48)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Invite Member")
            .setView(input)
            .setPositiveButton("Invite") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    inviteMember(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun inviteMember(email: String) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().inviteMember(repoId, mapOf("email" to email))
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Invite sent successfully!", Toast.LENGTH_SHORT).show()
                            loadMembers()
                        } else {
                            val msg = response.body()?.error?.message ?: "Failed to send invite"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun loadMembers() {
        if (repoId == -1L) return
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>().getMembers(repoId)
            .enqueue(object : Callback<ApiResponse<List<RepositoryMember>>> {
                override fun onResponse(call: Call<ApiResponse<List<RepositoryMember>>>, response: Response<ApiResponse<List<RepositoryMember>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            members.clear()
                            response.body()?.data?.let { members.addAll(it) }
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<RepositoryMember>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }
}
