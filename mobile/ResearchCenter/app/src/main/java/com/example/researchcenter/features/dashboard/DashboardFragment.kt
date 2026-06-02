package com.example.researchcenter.features.dashboard

import android.content.Intent
import android.os.Bundle
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
import com.example.researchcenter.features.profile.ActivityLogAdapter
import com.example.researchcenter.features.repository.CreateRepositoryBottomSheet
import com.example.researchcenter.features.repository.RepositoryAdapter
import com.example.researchcenter.features.repository.RepositoryDetailActivity
import com.example.researchcenter.shared.api.*
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.data.AppDatabase
import com.example.researchcenter.shared.data.RepositoryEntity
import com.example.researchcenter.shared.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardFragment : Fragment() {

    private lateinit var rvMyRepositories: RecyclerView
    private lateinit var rvJoinedRepositories: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var tvMyReposHeader: TextView
    private lateinit var tvJoinedReposHeader: TextView
    private lateinit var tvWelcome: TextView

    private val myRepositories = mutableListOf<Repository>()
    private val joinedRepositories = mutableListOf<Repository>()

    private lateinit var myAdapter: RepositoryAdapter
    private lateinit var joinedAdapter: RepositoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapters()
        loadMe()
        loadRepositories()
    }

    private fun initViews(view: View) {
        rvMyRepositories = view.findViewById(R.id.rv_my_repositories)
        rvJoinedRepositories = view.findViewById(R.id.rv_joined_repositories)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tvMyReposHeader = view.findViewById(R.id.tv_my_repos_header)
        tvJoinedReposHeader = view.findViewById(R.id.tv_joined_repos_header)
        tvWelcome = view.findViewById(R.id.tv_welcome)

        val sessionName = SessionManager.getName(requireContext())?.split(" ")?.firstOrNull() ?: "User"
        tvWelcome.text = getString(R.string.welcome_back, sessionName)

        swipeRefresh.setColorSchemeColors(resources.getColor(R.color.primary_green, null))
        swipeRefresh.setOnRefreshListener {
            loadRepositories()
        }

        view.findViewById<MaterialButton>(R.id.btn_create_repo).setOnClickListener {
            showCreateRepoSheet()
        }
    }

    private fun setupAdapters() {
        val currentUserId = SessionManager.getUserId(requireContext())

        myAdapter = RepositoryAdapter(
            myRepositories, currentUserId,
            onOpen = { openRepoDetails(it) },
            onEdit = { showEditRepoDialog(it) },
            onDelete = { confirmDeleteRepo(it) },
            onToggleBookmark = { toggleRepoBookmark(it) }
        )
        rvMyRepositories.layoutManager = LinearLayoutManager(context)
        rvMyRepositories.adapter = myAdapter

        joinedAdapter = RepositoryAdapter(
            joinedRepositories, currentUserId,
            onOpen = { openRepoDetails(it) },
            onEdit = { showEditRepoDialog(it) },
            onDelete = { confirmDeleteRepo(it) },
            onToggleBookmark = { toggleRepoBookmark(it) }
        )
        rvJoinedRepositories.layoutManager = LinearLayoutManager(context)
        rvJoinedRepositories.adapter = joinedAdapter
    }

    private fun openRepoDetails(repo: Repository) {
        val intent = Intent(context, RepositoryDetailActivity::class.java)
        intent.putExtra("REPO_ID", repo.id)
        intent.putExtra("REPO_NAME", repo.name)
        startActivity(intent)
    }

    private fun loadMe() {
        val ctx = context ?: return
        RetrofitClient.createService<AuthApi>().getMe().enqueue(object : Callback<ApiResponse<UserData>> {
            override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                val wrapper = response.body()
                if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                    activity?.runOnUiThread {
                        val result = wrapper.data
                        SessionManager.saveUserId(ctx, result.id)
                        SessionManager.saveEmail(ctx, result.email)
                        val fullName = "${result.firstname} ${result.lastname}".trim()
                        SessionManager.saveName(ctx, fullName)
                        SessionManager.saveRole(ctx, result.role)
                        tvWelcome.text = getString(R.string.welcome_back, result.firstname)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {}
        })
    }

    private fun loadRepositories() {
        val ctx = context ?: return
        swipeRefresh.isRefreshing = true

        // Show cached data first
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cached = AppDatabase.getInstance(ctx).repositoryDao().getAll().map { it.toModel() }
                if (cached.isNotEmpty()) {
                    withContext(Dispatchers.Main) { displayRepositories(cached) }
                }
            } catch (_: Exception) {}
        }

        RetrofitClient.createService<RepositoryApi>().getRepositories()
            .enqueue(object : Callback<ApiResponse<List<Repository>>> {
                override fun onResponse(call: Call<ApiResponse<List<Repository>>>, response: Response<ApiResponse<List<Repository>>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            val result = response.body()?.data ?: emptyList()
                            displayRepositories(result)
                            // Cache
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = AppDatabase.getInstance(ctx)
                                    db.repositoryDao().deleteAll()
                                    db.repositoryDao().insertAll(result.map { RepositoryEntity.fromModel(it) })
                                } catch (_: Exception) {}
                            }
                        } else {
                            Toast.makeText(ctx, "Failed to load repositories", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<Repository>>>, t: Throwable) {
                    activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
                }
            })
    }

    private fun displayRepositories(result: List<Repository>) {
        val currentUserId = SessionManager.getUserId(requireContext())
        myRepositories.clear()
        joinedRepositories.clear()

        result.forEach { repo ->
            if (repo.ownerId == currentUserId) myRepositories.add(repo)
            else joinedRepositories.add(repo)
        }

        // Sort bookmarked first
        val sortedMy = myRepositories.sortedWith(
            compareByDescending<Repository> { it.bookmarked }.thenByDescending { it.updatedAt ?: it.createdAt }
        )
        val sortedJoined = joinedRepositories.sortedWith(
            compareByDescending<Repository> { it.bookmarked }.thenByDescending { it.updatedAt ?: it.createdAt }
        )
        myRepositories.clear(); myRepositories.addAll(sortedMy)
        joinedRepositories.clear(); joinedRepositories.addAll(sortedJoined)

        myAdapter.notifyDataSetChanged()
        joinedAdapter.notifyDataSetChanged()

        val hasAny = myRepositories.isNotEmpty() || joinedRepositories.isNotEmpty()
        layoutEmptyState.visibility = if (hasAny) View.GONE else View.VISIBLE

        tvMyReposHeader.visibility = if (myRepositories.isNotEmpty()) View.VISIBLE else View.GONE
        tvMyReposHeader.text = getString(R.string.your_repositories, myRepositories.size)
        rvMyRepositories.visibility = if (myRepositories.isNotEmpty()) View.VISIBLE else View.GONE

        tvJoinedReposHeader.visibility = if (joinedRepositories.isNotEmpty()) View.VISIBLE else View.GONE
        tvJoinedReposHeader.text = getString(R.string.invited_repositories, joinedRepositories.size)
        rvJoinedRepositories.visibility = if (joinedRepositories.isNotEmpty()) View.VISIBLE else View.GONE
    }



    private fun showCreateRepoSheet() {
        CreateRepositoryBottomSheet { loadRepositories() }
            .show(childFragmentManager, "CreateRepo")
    }

    private fun showEditRepoDialog(repo: Repository) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_repository, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_repo_name)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_repo_desc)
        view.findViewById<View>(R.id.tv_title)?.visibility = View.GONE
        view.findViewById<View>(R.id.layout_buttons)?.visibility = View.GONE
        etName.setText(repo.name)
        etDesc.setText(repo.description ?: "")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_repository)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "Repository name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    updateRepository(repo.id, name, desc)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateRepository(id: Long, name: String, desc: String) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>()
            .updateRepository(id, mapOf("name" to name, "description" to desc))
            .enqueue(object : Callback<ApiResponse<Repository>> {
                override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "Repository updated", Toast.LENGTH_SHORT).show()
                            loadRepositories()
                        } else {
                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                    activity?.runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun confirmDeleteRepo(repo: Repository) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_repository)
            .setMessage(getString(R.string.delete_repo_message, repo.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                swipeRefresh.isRefreshing = true
                RetrofitClient.createService<RepositoryApi>().deleteRepository(repo.id)
                    .enqueue(object : Callback<ApiResponse<Any>> {
                        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                            activity?.runOnUiThread {
                                swipeRefresh.isRefreshing = false
                                if (response.isSuccessful) loadRepositories()
                                else Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
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
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toggleRepoBookmark(repo: Repository) {
        RetrofitClient.createService<RepositoryApi>().toggleBookmark(repo.id)
            .enqueue(object : Callback<ApiResponse<BookmarkToggleResponse>> {
                override fun onResponse(call: Call<ApiResponse<BookmarkToggleResponse>>, response: Response<ApiResponse<BookmarkToggleResponse>>) {
                    if (response.isSuccessful) activity?.runOnUiThread { loadRepositories() }
                }
                override fun onFailure(call: Call<ApiResponse<BookmarkToggleResponse>>, t: Throwable) {}
            })
    }

    override fun onResume() {
        super.onResume()
        if (::swipeRefresh.isInitialized) {
            loadRepositories()
        }
    }
}
