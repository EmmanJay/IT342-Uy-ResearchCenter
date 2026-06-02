package com.example.researchcenter.features.repository

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.researchcenter.R
import com.example.researchcenter.features.repository.tabs.*
import com.example.researchcenter.shared.api.RepositoryApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.Repository
import com.example.researchcenter.shared.ui.BreadcrumbsView
import com.example.researchcenter.shared.ui.UserAvatarView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepositoryDetailActivity : AppCompatActivity() {

    private var repoId: Long = -1
    private var isOwner: Boolean = false
    private var repoName: String = ""
    private var repoDescription: String = ""

    private lateinit var tvRepoName: TextView
    private lateinit var btnLeaveRepo: MaterialButton
    private lateinit var tvAvatar: UserAvatarView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnRepoInfo: android.widget.ImageButton
    private lateinit var btnFloatingAction: com.google.android.material.floatingactionbutton.FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository_detail)

        repoId = intent.getLongExtra("REPO_ID", -1L).takeIf { it != -1L }
            ?: intent.getLongExtra("id", -1L).takeIf { it != -1L }
            ?: intent.getLongExtra("repo_id", -1L)

        repoName = intent.getStringExtra("REPO_NAME") ?: "Repository"

        if (repoId == -1L) {
            Toast.makeText(this, "Invalid Repository ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupTopBar()
        setupViewPager()
        loadRepositoryDetails()
    }

    private fun initViews() {
        tvRepoName = findViewById(R.id.tv_repo_name)
        btnLeaveRepo = findViewById(R.id.btn_leave_repo)
        tvAvatar = findViewById(R.id.tv_avatar)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        btnRepoInfo = findViewById(R.id.btn_repo_info)
        btnFloatingAction = findViewById(R.id.btn_floating_action)

        tvRepoName.text = repoName

        btnRepoInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(repoName)
                .setMessage(if (repoDescription.isNotEmpty()) repoDescription else "No description provided.")
                .setPositiveButton("Close", null)
                .show()
        }

        btnFloatingAction.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> {
                    val intent = Intent(this, com.example.researchcenter.features.material.AddMaterialActivity::class.java).apply {
                        putExtra("REPO_ID", repoId)
                        putExtra("REPO_NAME", repoName)
                    }
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this, com.example.researchcenter.features.request.NewRequestActivity::class.java).apply {
                        putExtra("REPO_ID", repoId)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupTopBar() {
        val name = SessionManager.getName(this)
        val email = SessionManager.getEmail(this)
        tvAvatar.setUser(name, email, SessionManager.getProfilePicture(this))
    }

    private fun setupViewPager() {
        val adapter = RepositoryTabsAdapter(this, repoId, isOwner)
        viewPager.adapter = adapter

        val tabTitles = arrayOf("Materials", "Bookmarks", "Requests", "Members", "Updates", "Activity")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFabVisibility(position)
            }
        })
    }

    private fun updateFabVisibility(position: Int) {
        if (!::btnFloatingAction.isInitialized) return
        when (position) {
            0 -> {
                btnFloatingAction.visibility = if (isOwner) View.VISIBLE else View.GONE
            }
            2 -> {
                btnFloatingAction.visibility = View.VISIBLE
            }
            else -> {
                btnFloatingAction.visibility = View.GONE
            }
        }
    }

    private fun loadRepositoryDetails() {
        RetrofitClient.createService<RepositoryApi>().getRepository(repoId)
            .enqueue(object : Callback<ApiResponse<Repository>> {
                override fun onResponse(call: Call<ApiResponse<Repository>>, response: Response<ApiResponse<Repository>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val repo = response.body()?.data
                        if (repo != null) {
                            runOnUiThread {
                                setupRepoDetails(repo)
                            }
                        }
                    } else {
                        Toast.makeText(this@RepositoryDetailActivity, "Failed to load repository details", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Repository>>, t: Throwable) {
                    Toast.makeText(this@RepositoryDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupRepoDetails(repo: Repository) {
        repoName = repo.name
        tvRepoName.text = repo.name
        repoDescription = repo.description ?: ""

        val currentUserId = SessionManager.getUserId(this)
        isOwner = repo.ownerId == currentUserId || repo.role == "OWNER"

        // Leave repository button visibility
        if (!isOwner) {
            btnLeaveRepo.visibility = View.VISIBLE
            btnLeaveRepo.setOnClickListener {
                confirmLeaveRepository()
            }
        } else {
            btnLeaveRepo.visibility = View.GONE
        }

        // Update children fragments with the loaded isOwner status
        updateFragmentsOwnerStatus(isOwner)
        updateFabVisibility(viewPager.currentItem)
    }

    private fun updateFragmentsOwnerStatus(owner: Boolean) {
        for (i in 0 until 6) {
            val fragment = supportFragmentManager.findFragmentByTag("f$i")
            when (fragment) {
                is MaterialsFragment -> fragment.setIsOwner(owner)
                is BookmarksFragment -> fragment.setIsOwner(owner)
                is RequestsFragment -> fragment.setIsOwner(owner)
                is MembersFragment -> fragment.setIsOwner(owner)
                is UpdatesFragment -> fragment.setIsOwner(owner)
                is RepoActivityFragment -> fragment.setIsOwner(owner)
            }
        }
    }

    private fun confirmLeaveRepository() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Leave Repository")
            .setMessage("Are you sure you want to leave this repository?")
            .setPositiveButton("Leave") { _, _ ->
                leaveRepository()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveRepository() {
        RetrofitClient.createService<RepositoryApi>().leaveRepository(repoId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@RepositoryDetailActivity, "Left repository successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RepositoryDetailActivity, "Failed to leave repository", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@RepositoryDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private inner class RepositoryTabsAdapter(
        activity: AppCompatActivity,
        private val repoId: Long,
        private val isOwner: Boolean
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 6

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MaterialsFragment.newInstance(repoId, isOwner)
                1 -> BookmarksFragment.newInstance(repoId, isOwner)
                2 -> RequestsFragment.newInstance(repoId, isOwner)
                3 -> MembersFragment.newInstance(repoId, isOwner)
                4 -> UpdatesFragment.newInstance(repoId, isOwner)
                5 -> RepoActivityFragment.newInstance(repoId, isOwner)
                else -> throw IllegalStateException("Invalid tab position: $position")
            }
        }
    }
}