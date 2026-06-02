package com.example.researchcenter.features.repository.tabs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.RepositoryMember
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MembersFragment : Fragment() {
    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private val allMembers = mutableListOf<RepositoryMember>()
    private val displayedMembers = mutableListOf<RepositoryMember>()
    private lateinit var adapter: MemberAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvMembers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnInvite: View
    private lateinit var llInviteSection: View
    private lateinit var etSearch: TextInputEditText

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
        updateInviteSectionVisibility()
    }

    private fun updateInviteSectionVisibility() {
        if (::llInviteSection.isInitialized) {
            llInviteSection.visibility = if (isOwner) View.VISIBLE else View.GONE
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
        btnInvite = view.findViewById(R.id.btn_invite_member)
        llInviteSection = view.findViewById(R.id.ll_invite_section)
        etSearch = view.findViewById(R.id.et_search)

        updateInviteSectionVisibility()
        btnInvite.setOnClickListener { showInviteDialog() }
        
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterMembers(s.toString().trim())
            }
        })
        
        adapter = MemberAdapter(displayedMembers)
        rvMembers.layoutManager = LinearLayoutManager(context)
        rvMembers.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadMembers() }
        loadMembers()
        
        return view
    }

    private fun filterMembers(query: String) {
        val filtered = if (query.isEmpty()) {
            allMembers
        } else {
            val lower = query.lowercase(Locale.getDefault())
            allMembers.filter {
                it.email.lowercase(Locale.getDefault()).contains(lower) ||
                it.name.lowercase(Locale.getDefault()).contains(lower) ||
                it.role.lowercase(Locale.getDefault()).contains(lower)
            }
        }
        displayedMembers.clear()
        displayedMembers.addAll(filtered)
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (displayedMembers.isEmpty()) View.VISIBLE else View.GONE
    }

    private var inviteDialog: androidx.appcompat.app.AlertDialog? = null

    private fun showInviteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_member_search, null)
        val etMemberSearch = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etMemberSearch)
        val rvSearchResults = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSearchResults)
        val btnCancel = dialogView.findViewById<android.view.View>(R.id.btnMemberInviteCancel)

        val searchResults = mutableListOf<com.example.researchcenter.shared.model.User>()
        val searchAdapter = UserSearchAdapter(searchResults) { selectedUser ->
            inviteMember(selectedUser.email)
            inviteDialog?.dismiss()
        }
        rvSearchResults.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        rvSearchResults.adapter = searchAdapter

        inviteDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { inviteDialog?.dismiss() }

        etMemberSearch.addTextChangedListener(object : android.text.TextWatcher {
            private var searchRunnable: Runnable? = null
            private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s.toString().trim()
                if (query.length >= 3) {
                    searchRunnable = Runnable {
                        performUserSearch(query, searchAdapter, searchResults)
                    }
                    searchHandler.postDelayed(searchRunnable!!, 400)
                } else {
                    searchResults.clear()
                    searchAdapter.notifyDataSetChanged()
                }
            }
        })

        inviteDialog?.show()
    }

    private fun performUserSearch(
        query: String,
        adapter: UserSearchAdapter,
        list: MutableList<com.example.researchcenter.shared.model.User>
    ) {
        RetrofitClient.createService<com.example.researchcenter.shared.api.UserApi>().searchUsers(query)
            .enqueue(object : retrofit2.Callback<ApiResponse<com.example.researchcenter.shared.model.User>> {
                override fun onResponse(
                    call: retrofit2.Call<ApiResponse<com.example.researchcenter.shared.model.User>>,
                    response: retrofit2.Response<ApiResponse<com.example.researchcenter.shared.model.User>>
                ) {
                    activity?.runOnUiThread {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val user = response.body()?.data
                            list.clear()
                            if (user != null) {
                                list.add(user)
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<ApiResponse<com.example.researchcenter.shared.model.User>>,
                    t: Throwable
                ) {
                    // silently handle errors
                }
            })
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
                            allMembers.clear()
                            response.body()?.data?.let { allMembers.addAll(it) }
                            filterMembers(etSearch.text?.toString() ?: "")
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<RepositoryMember>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }

    class UserSearchAdapter(
        private val users: List<com.example.researchcenter.shared.model.User>,
        private val onInviteClick: (com.example.researchcenter.shared.model.User) -> Unit
    ) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvInviteName)
            val tvEmail: TextView = view.findViewById(R.id.tvInviteEmail)
            val btnInvite: View = view.findViewById(R.id.btnInviteUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_invite_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            val fullName = "${user.firstname} ${user.lastname}".trim()
            holder.tvName.text = if (fullName.isNotEmpty()) fullName else user.email
            holder.tvEmail.text = user.email
            holder.btnInvite.setOnClickListener { onInviteClick(user) }
        }

        override fun getItemCount() = users.size
    }
}
