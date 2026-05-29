package com.example.researchcenter.features.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.features.main.MainActivity
import com.example.researchcenter.shared.api.ActivityApi
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.*
import com.example.researchcenter.shared.ui.UserAvatarView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var tvStatMemberSince: TextView
    private lateinit var tvStatRepos: TextView
    private lateinit var tvStatDays: TextView
    private lateinit var avatarProfile: UserAvatarView
    private lateinit var rvActivities: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val activities = mutableListOf<ActivityLog>()
    private lateinit var activityAdapter: ActivityLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tv_profile_name)
        tvEmail = view.findViewById(R.id.tv_profile_email)
        tvRoleBadge = view.findViewById(R.id.tv_role_badge)
        tvStatMemberSince = view.findViewById(R.id.tv_stat_member_since)
        tvStatRepos = view.findViewById(R.id.tv_stat_repos)
        tvStatDays = view.findViewById(R.id.tv_stat_days)
        avatarProfile = view.findViewById(R.id.avatar_profile)
        rvActivities = view.findViewById(R.id.rv_activity_log)
        progressBar = view.findViewById(R.id.progress_activity)

        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)

        // Set cached values first
        val sessionName = SessionManager.getName(requireContext()) ?: "User"
        val sessionEmail = SessionManager.getEmail(requireContext()) ?: ""
        tvName.text = sessionName
        tvEmail.text = sessionEmail
        avatarProfile.setUser(sessionName, sessionEmail)
        tvRoleBadge.text = SessionManager.getRole(requireContext()) ?: "USER"

        activityAdapter = ActivityLogAdapter(activities)
        rvActivities.layoutManager = LinearLayoutManager(context)
        rvActivities.adapter = activityAdapter

        btnEdit.setOnClickListener { showEditProfileDialog() }
        btnLogout.setOnClickListener {
            SessionManager.clearSession(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        loadMe()
        loadActivities()
    }

    private fun loadMe() {
        RetrofitClient.createService<AuthApi>().getMe().enqueue(object : Callback<ApiResponse<UserData>> {
            override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                val wrapper = response.body()
                if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                    activity?.runOnUiThread {
                        val user = wrapper.data
                        val ctx = context ?: return@runOnUiThread
                        SessionManager.saveUserId(ctx, user.id)
                        SessionManager.saveEmail(ctx, user.email)
                        val fullName = "${user.firstname} ${user.lastname}".trim()
                        SessionManager.saveName(ctx, fullName)
                        SessionManager.saveRole(ctx, user.role)

                        tvName.text = fullName
                        tvEmail.text = user.email
                        tvRoleBadge.text = user.role
                        avatarProfile.setUser(fullName, user.email)

                        // Stats
                        user.createdAt?.let { dateStr ->
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val date = parser.parse(dateStr.take(19))
                                if (date != null) {
                                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    tvStatMemberSince.text = getString(R.string.member_since, formatter.format(date))
                                    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time)
                                    tvStatDays.text = getString(R.string.days_active, days.toInt())
                                }
                            } catch (_: Exception) {
                                tvStatMemberSince.text = getString(R.string.member_since, dateStr.take(10))
                            }
                        }

                        (activity as? MainActivity)?.refreshAvatar()
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {}
        })
    }

    private fun showEditProfileDialog() {
        val parts = (SessionManager.getName(requireContext()) ?: " ").split(" ", limit = 2)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null)
        val etFirst = view.findViewById<TextInputEditText>(R.id.et_first_name)
        val etLast = view.findViewById<TextInputEditText>(R.id.et_last_name)
        etFirst.setText(parts.getOrElse(0) { "" })
        etLast.setText(parts.getOrElse(1) { "" })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val first = etFirst.text.toString().trim()
                val last = etLast.text.toString().trim()
                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(context, "Name fields cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveProfile(first, last)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveProfile(firstname: String, lastname: String) {
        RetrofitClient.createService<AuthApi>()
            .updateProfile(UpdateProfileRequest(firstname, lastname))
            .enqueue(object : Callback<ApiResponse<UserData>> {
                override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        activity?.runOnUiThread {
                            val user = wrapper.data
                            val fullName = "${user.firstname} ${user.lastname}".trim()
                            val ctx = context ?: return@runOnUiThread
                            SessionManager.saveName(ctx, fullName)
                            tvName.text = fullName
                            avatarProfile.setUser(fullName, user.email)
                            Toast.makeText(ctx, "Profile updated", Toast.LENGTH_SHORT).show()
                            (activity as? MainActivity)?.refreshAvatar()
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Could not update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun loadActivities() {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.createService<ActivityApi>().getActivities(0, 10)
            .enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
                override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                    activity?.runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.success == true) {
                            activities.clear()
                            response.body()?.data?.let { activities.addAll(it) }
                            activityAdapter.notifyDataSetChanged()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                    activity?.runOnUiThread { progressBar.visibility = View.GONE }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (::tvName.isInitialized) {
            loadMe()
            loadActivities()
        }
    }
}
