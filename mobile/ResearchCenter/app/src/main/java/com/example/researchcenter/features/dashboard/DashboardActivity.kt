package com.example.researchcenter.features.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.researchcenter.R

import com.example.researchcenter.features.profile.ProfileActivity
import com.example.researchcenter.features.repository.RepositoryAdapter
import com.example.researchcenter.features.repository.RepositoryDetailActivity
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.Repository
import com.example.researchcenter.shared.model.UserData
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.data.AppDatabase
import com.example.researchcenter.shared.data.RepositoryEntity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.example.researchcenter.shared.api.NotificationWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var rvMyRepositories: RecyclerView
    private lateinit var rvJoinedRepositories: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var tvMyReposHeader: TextView
    private lateinit var tvJoinedReposHeader: TextView

    private val myRepositories = mutableListOf<Repository>()
    private val joinedRepositories = mutableListOf<Repository>()

    private lateinit var myAdapter: RepositoryAdapter
    private lateinit var joinedAdapter: RepositoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupAdapters()
        setupBottomNavigation()
        connectWebSocket()
        loadMe()
        loadRepositories()
    }

    private fun initViews() {
        rvMyRepositories = findViewById(R.id.rv_my_repositories)
        rvJoinedRepositories = findViewById(R.id.rv_joined_repositories)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        tvMyReposHeader = findViewById(R.id.tv_my_repos_header)
        tvJoinedReposHeader = findViewById(R.id.tv_joined_repos_header)
        tvJoinedReposHeader = findViewById(R.id.tv_joined_repos_header)

        swipeRefresh.setOnRefreshListener {
            loadRepositories()
        }

        findViewById<MaterialButton>(R.id.btn_create_repo).setOnClickListener {
            showCreateRepoDialog()
        }

        // Admin disabled per spec

        val tvAvatar = findViewById<com.example.researchcenter.shared.ui.UserAvatarView>(R.id.tv_avatar)
        val name = SessionManager.getName(this)
        val email = SessionManager.getEmail(this)
        tvAvatar.setUser(name, email)
        tvAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupAdapters() {
        val currentUserId = SessionManager.getUserId(this)
        val repoCallbacks = repositoryAdapterCallbacks(currentUserId)

        myAdapter = RepositoryAdapter(
            myRepositories,
            currentUserId,
            onOpen = { openRepoDetails(it) },
            onEdit = repoCallbacks.onEdit,
            onDelete = repoCallbacks.onDelete,
            onToggleBookmark = repoCallbacks.onToggleBookmark
        )
        rvMyRepositories.layoutManager = LinearLayoutManager(this)
        rvMyRepositories.adapter = myAdapter

        joinedAdapter = RepositoryAdapter(
            joinedRepositories,
            currentUserId,
            onOpen = { openRepoDetails(it) },
            onEdit = repoCallbacks.onEdit,
            onDelete = repoCallbacks.onDelete,
            onToggleBookmark = repoCallbacks.onToggleBookmark
        )
        rvJoinedRepositories.layoutManager = LinearLayoutManager(this)
        rvJoinedRepositories.adapter = joinedAdapter
    }

    private data class RepoCallbacks(
        val onEdit: (Repository) -> Unit,
        val onDelete: (Repository) -> Unit,
        val onToggleBookmark: (Repository) -> Unit
    )

    private fun repositoryAdapterCallbacks(currentUserId: Long): RepoCallbacks {
        return RepoCallbacks(
            onEdit = { repo -> showEditRepoDialog(repo) },
            onDelete = { repo -> confirmDeleteRepo(repo) },
            onToggleBookmark = { repo -> toggleRepoBookmark(repo) }
        )
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            // Bottom navigation handled here
            false
        }
    }

    private fun openRepoDetails(repo: Repository) {
        val intent = Intent(this, RepositoryDetailActivity::class.java)
        intent.putExtra("REPO_ID", repo.id)
        intent.putExtra("REPO_NAME", repo.name)
        startActivity(intent)
    }

    private fun loadMe() {
        val authApi = RetrofitClient.createService<AuthApi>()
        authApi.getMe().enqueue(object : Callback<ApiResponse<UserData>> {
            override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                val wrapper = response.body()
                if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                    val result = wrapper.data
                    runOnUiThread {
                        SessionManager.saveUserId(this@DashboardActivity, result.id)
                        SessionManager.saveEmail(this@DashboardActivity, result.email)
                        val fullName = "${result.firstname} ${result.lastname}".trim()
                        SessionManager.saveName(this@DashboardActivity, fullName)
                        SessionManager.saveRole(this@DashboardActivity, result.role)
                        
                        
                        findViewById<com.example.researchcenter.shared.ui.UserAvatarView>(R.id.tv_avatar).setUser(fullName, result.email)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {}
        })
    }

    private fun loadRepositories() {
        swipeRefresh.isRefreshing = true

        // 1. Show cached data immediately
        CoroutineScope(Dispatchers.IO).launch {
            val cached = AppDatabase.getInstance(this@DashboardActivity)
                .repositoryDao().getAll().map { it.toModel() }
            if (cached.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    displayRepositories(cached)
                }
            }
        }

        // 2. Fetch fresh data from API
        val repositoryApi = RetrofitClient.createService<RepositoryApi>()
        repositoryApi.getRepositories().enqueue(object : Callback<ApiResponse<List<Repository>>> {
            override fun onResponse(call: Call<ApiResponse<List<Repository>>>, response: Response<ApiResponse<List<Repository>>>) {
                val wrapper = response.body()
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        val result = wrapper.data
                        displayRepositories(result)

                        // 3. Cache to Room
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getInstance(this@DashboardActivity)
                            db.repositoryDao().deleteAll()
                            db.repositoryDao().insertAll(result.map { RepositoryEntity.fromModel(it) })
                        }
                    } else {
                        val errorMsg = wrapper?.error?.message ?: "Failed to load repositories"
                        Toast.makeText(this@DashboardActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Repository>>>, t: Throwable) {
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    // If we already showed cached data, just show a mild warning
                    if (myRepositories.isEmpty() && joinedRepositories.isEmpty()) {
                        Toast.makeText(this@DashboardActivity, "Offline — showing cached data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun sortBookmarkedFirst(items: List<Repository>): List<Repository> =
        items.sortedWith(
            compareByDescending<Repository> { it.bookmarked }
                .thenByDescending { it.updatedAt ?: it.createdAt }
        )

    private fun displayRepositories(result: List<Repository>) {
        val currentUserId = SessionManager.getUserId(this)

        myRepositories.clear()
        joinedRepositories.clear()

        result.forEach { repo ->
            if (repo.ownerId == currentUserId) {
                myRepositories.add(repo)
            } else {
                joinedRepositories.add(repo)
            }
        }

        val sortedMy = sortBookmarkedFirst(myRepositories)
        val sortedJoined = sortBookmarkedFirst(joinedRepositories)
        myRepositories.clear()
        myRepositories.addAll(sortedMy)
        joinedRepositories.clear()
        joinedRepositories.addAll(sortedJoined)

        setupAdapters()
        myAdapter.notifyDataSetChanged()
        joinedAdapter.notifyDataSetChanged()

        val hasAny = myRepositories.isNotEmpty() || joinedRepositories.isNotEmpty()
        layoutEmptyState.visibility = if (hasAny) View.GONE else View.VISIBLE

        tvMyReposHeader.visibility = if (myRepositories.isNotEmpty()) View.VISIBLE else View.GONE
        rvMyRepositories.visibility = if (myRepositories.isNotEmpty()) View.VISIBLE else View.GONE

        tvJoinedReposHeader.visibility = if (joinedRepositories.isNotEmpty()) View.VISIBLE else View.GONE
        rvJoinedRepositories.visibility = if (joinedRepositories.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCreateRepoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_repository, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_repo_name)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_repo_desc)
        view.findViewById<View>(R.id.tv_title)?.visibility = View.GONE
        view.findViewById<View>(R.id.layout_buttons)?.visibility = View.GONE

        MaterialAlertDialogBuilder(this)
            .setTitle("Create Repository")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isNotEmpty()) {
                    createRepository(name, desc)
                } else {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditRepoDialog(repo: Repository) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_repository, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_repo_name)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_repo_desc)
        view.findViewById<View>(R.id.tv_title)?.visibility = View.GONE
        view.findViewById<View>(R.id.layout_buttons)?.visibility = View.GONE
        etName.setText(repo.name)
        etDesc.setText(repo.description ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Repository")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isNotEmpty()) {
                    updateRepository(repo.id, name, desc)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRepository(id: Long, name: String, desc: String) {
        swipeRefresh.isRefreshing = true
        RetrofitClient.createService<RepositoryApi>()
            .updateRepository(id, mapOf("name" to name, "description" to desc))
            .enqueue(object : Callback<ApiResponse<Repository>> {
                override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                    runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@DashboardActivity, "Repository updated", Toast.LENGTH_SHORT).show()
                            loadRepositories()
                        } else {
                            Toast.makeText(this@DashboardActivity, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                    runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@DashboardActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun confirmDeleteRepo(repo: Repository) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Repository")
            .setMessage("Delete \"${repo.name}\"? This removes the repository for everyone.")
            .setPositiveButton("Delete") { _, _ ->
                swipeRefresh.isRefreshing = true
                RetrofitClient.createService<RepositoryApi>()
                    .deleteRepository(repo.id)
                    .enqueue(object : Callback<ApiResponse<Any>> {
                        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                            runOnUiThread {
                                swipeRefresh.isRefreshing = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    loadRepositories()
                                } else {
                                    Toast.makeText(this@DashboardActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                            runOnUiThread {
                                swipeRefresh.isRefreshing = false
                                Toast.makeText(this@DashboardActivity, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleRepoBookmark(repo: Repository) {
        RetrofitClient.createService<RepositoryApi>()
            .toggleBookmark(repo.id)
            .enqueue(object : Callback<ApiResponse<com.example.researchcenter.shared.model.BookmarkToggleResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.researchcenter.shared.model.BookmarkToggleResponse>>,
                    response: Response<ApiResponse<com.example.researchcenter.shared.model.BookmarkToggleResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        runOnUiThread { loadRepositories() }
                    }
                }
                override fun onFailure(
                    call: Call<ApiResponse<com.example.researchcenter.shared.model.BookmarkToggleResponse>>,
                    t: Throwable
                ) {}
            })
    }

    private fun createRepository(name: String, desc: String) {
        swipeRefresh.isRefreshing = true
        val repositoryApi = RetrofitClient.createService<RepositoryApi>()
        val body = mapOf("name" to name, "description" to desc)
        
        repositoryApi.createRepository(body).enqueue(object : Callback<ApiResponse<Repository>> {
            override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                val wrapper = response.body()
                runOnUiThread {
                    if (response.isSuccessful && wrapper?.success == true) {
                        Toast.makeText(this@DashboardActivity, "Repository created", Toast.LENGTH_SHORT).show()
                        loadRepositories()
                    } else {
                        swipeRefresh.isRefreshing = false
                        val errorMsg = wrapper?.error?.message ?: "Failed to create repository"
                        Toast.makeText(this@DashboardActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@DashboardActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadRepositories()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationWebSocketClient.removeListener(wsListener)
    }

    private val wsListener = object : NotificationWebSocketClient.NotificationListener {
        override fun onNotificationReceived(type: String, message: String, data: JSONObject?) {
            // Refresh dashboard when we receive a real-time notification
            runOnUiThread {
                if (type in listOf("REPO_UPDATED", "MATERIAL_ADDED", "MEMBER_INVITED", "REQUEST_CREATED", "REPOSITORY_NOTE_UPDATED")) {
                    loadRepositories()
                }
                if (message.isNotBlank()) {
                    Toast.makeText(this@DashboardActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onConnectionStateChanged(connected: Boolean) {
            // Could show a connectivity indicator in the future
        }
    }

    private fun connectWebSocket() {
        val token = SessionManager.getToken(this) ?: return
        NotificationWebSocketClient.addListener(wsListener)
        NotificationWebSocketClient.connect(token)
    }
}
