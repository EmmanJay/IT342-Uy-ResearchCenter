package com.example.researchcenter.features.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.shared.api.ActivityApi
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ActivityLog
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.UpdateProfileRequest
import com.example.researchcenter.shared.model.UserData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAvatarInitials: TextView
    private val activities = mutableListOf<ActivityLog>()
    private lateinit var adapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvName = findViewById(R.id.tv_profile_name)
        tvEmail = findViewById(R.id.tv_profile_email)
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnEdit = findViewById<Button>(R.id.btn_edit_profile)

        rvActivities = findViewById(R.id.rv_activity_log)
        progressBar = findViewById(R.id.progress_activity)

        val sessionName = SessionManager.getName(this) ?: "ResearchCenter User"
        tvName.text = sessionName
        tvEmail.text = SessionManager.getEmail(this) ?: "No email"
        tvAvatarInitials.text = if (sessionName.isNotBlank()) sessionName.take(1).uppercase() else "U"

        adapter = ActivityLogAdapter(activities)
        rvActivities.layoutManager = LinearLayoutManager(this)
        rvActivities.adapter = adapter

        btnEdit.setOnClickListener { showEditProfileDialog() }

        btnLogout.setOnClickListener {
            SessionManager.clearSession(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadMe()
        loadActivities()
    }

    private fun loadMe() {
        RetrofitClient.createService<AuthApi>().getMe().enqueue(object : Callback<ApiResponse<UserData>> {
            override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                val wrapper = response.body()
                if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                    val user = wrapper.data
                    runOnUiThread {
                        SessionManager.saveUserId(this@ProfileActivity, user.id)
                        SessionManager.saveEmail(this@ProfileActivity, user.email)
                        SessionManager.saveName(this@ProfileActivity, "${user.firstname} ${user.lastname}".trim())
                        SessionManager.saveRole(this@ProfileActivity, user.role)
                        val fullName = "${user.firstname} ${user.lastname}".trim()
                        tvName.text = fullName
                        tvEmail.text = user.email
                        tvAvatarInitials.text = if (fullName.isNotEmpty()) fullName.take(1).uppercase() else "U"
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {}
        })
    }

    private fun showEditProfileDialog() {
        val parts = (SessionManager.getName(this) ?: " ").split(" ", limit = 2)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etFirst = view.findViewById<TextInputEditText>(R.id.et_first_name)
        val etLast = view.findViewById<TextInputEditText>(R.id.et_last_name)
        etFirst.setText(parts.getOrElse(0) { "" })
        etLast.setText(parts.getOrElse(1) { "" })

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Profile")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val first = etFirst.text.toString().trim()
                val last = etLast.text.toString().trim()
                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(this, "Name fields cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveProfile(first, last)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfile(firstname: String, lastname: String) {
        RetrofitClient.createService<AuthApi>()
            .updateProfile(UpdateProfileRequest(firstname, lastname))
            .enqueue(object : Callback<ApiResponse<UserData>> {
                override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        runOnUiThread {
                            val user = wrapper.data
                            val fullName = "${user.firstname} ${user.lastname}".trim()
                            SessionManager.saveName(this@ProfileActivity, fullName)
                            tvName.text = fullName
                            tvAvatarInitials.text = if (fullName.isNotEmpty()) fullName.take(1).uppercase() else "U"
                            Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Could not update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun loadActivities() {
        progressBar.visibility = View.VISIBLE
        rvActivities.visibility = View.GONE

        RetrofitClient.createService<ActivityApi>()
            .getActivities(0, 20)
            .enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ActivityLog>>>,
                    response: Response<ApiResponse<List<ActivityLog>>>
                ) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        val wrapper = response.body()
                        if (response.isSuccessful && wrapper?.success == true) {
                            activities.clear()
                            wrapper.data?.let { activities.addAll(it) }
                            adapter.notifyDataSetChanged()
                            rvActivities.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this@ProfileActivity, "Could not load activities", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@ProfileActivity, "Could not load activities", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }
}
