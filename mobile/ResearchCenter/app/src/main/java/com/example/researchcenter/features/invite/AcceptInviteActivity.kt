package com.example.researchcenter.features.invite

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.features.main.MainActivity
import com.example.researchcenter.features.repository.RepositoryDetailActivity
import com.example.researchcenter.shared.api.InviteApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.AcceptInviteResponse
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.InvitePreview
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class   AcceptInviteActivity : AppCompatActivity() {

    private var token: String? = null
    private var repositoryId: Long? = null
    private var repositoryName: String? = null

    private lateinit var tvStatusMessage: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutActions: LinearLayout
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnAccept: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept_invite)

        tvStatusMessage = findViewById(R.id.tv_status_message)
        pbLoading = findViewById(R.id.pb_loading)
        layoutActions = findViewById(R.id.layout_actions)
        btnCancel = findViewById(R.id.btn_cancel)
        btnAccept = findViewById(R.id.btn_accept)

        // Parse token from Intent extras or Uri deep link query
        val uri = intent.data
        token = intent.getStringExtra("token")
        if (token.isNullOrBlank() && uri != null) {
            token = uri.getQueryParameter("token")
        }

        if (token.isNullOrBlank()) {
            showError("Invalid invitation token.")
            return
        }

        // Authenticate check: If not logged in, redirect to login activity
        if (!SessionManager.isLoggedIn(this)) {
            SessionManager.clearSession(this)
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                putExtra("INVITE_TOKEN", token)
            }
            startActivity(loginIntent)
            finish()
            return
        }

        setupListeners()
        fetchInvitationDetails()
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener {
            rejectInvite()
        }

        btnAccept.setOnClickListener {
            acceptInvite()
        }
    }

    private fun fetchInvitationDetails() {
        val inviteToken = token ?: return
        showLoading(true)

        val inviteApi = RetrofitClient.createService<InviteApi>()
        inviteApi.getInvitation(inviteToken).enqueue(object : Callback<ApiResponse<InvitePreview>> {
            override fun onResponse(
                call: Call<ApiResponse<InvitePreview>>,
                response: Response<ApiResponse<InvitePreview>>
            ) {
                runOnUiThread {
                    showLoading(false)
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        val invitePreview = wrapper.data
                        repositoryId = invitePreview.repositoryId
                        repositoryName = invitePreview.repositoryName

                        val currentUserEmail = SessionManager.getEmail(this@AcceptInviteActivity)
                        if (currentUserEmail != null && currentUserEmail.lowercase() != invitePreview.email.lowercase()) {
                            // If currently logged-in user is different from the invited email, log out and redirect
                            Toast.makeText(this@AcceptInviteActivity, "This invitation belongs to a different email. Redirecting...", Toast.LENGTH_LONG).show()
                            SessionManager.clearSession(this@AcceptInviteActivity)
                            val loginIntent = Intent(this@AcceptInviteActivity, LoginActivity::class.java).apply {
                                putExtra("INVITE_TOKEN", inviteToken)
                            }
                            startActivity(loginIntent)
                            finish()
                            return@runOnUiThread
                        }

                        tvStatusMessage.text = "Accept the invitation to join ${invitePreview.repositoryName} as ${invitePreview.email}."
                        layoutActions.visibility = View.VISIBLE
                    } else {
                        val statusCode = response.code()
                        if (statusCode == 401 || statusCode == 403) {
                            // Token expired or invalid session; re-login
                            SessionManager.clearSession(this@AcceptInviteActivity)
                            val loginIntent = Intent(this@AcceptInviteActivity, LoginActivity::class.java).apply {
                                putExtra("INVITE_TOKEN", inviteToken)
                            }
                            startActivity(loginIntent)
                            finish()
                        } else {
                            val errorMsg = wrapper?.error?.message ?: "Failed to find invitation details."
                            showError(errorMsg)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<InvitePreview>>, t: Throwable) {
                runOnUiThread {
                    showLoading(false)
                    showError("Network error. Please check your connection and try again.")
                }
            }
        })
    }

    private fun acceptInvite() {
        val inviteToken = token ?: return
        btnAccept.isEnabled = false
        btnCancel.isEnabled = false
        tvStatusMessage.text = "Accepting invitation..."
        pbLoading.visibility = View.VISIBLE

        val inviteApi = RetrofitClient.createService<InviteApi>()
        inviteApi.acceptInvitation(inviteToken).enqueue(object : Callback<ApiResponse<AcceptInviteResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<AcceptInviteResponse>>,
                response: Response<ApiResponse<AcceptInviteResponse>>
            ) {
                runOnUiThread {
                    pbLoading.visibility = View.GONE
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        Toast.makeText(this@AcceptInviteActivity, wrapper.data.message ?: "Invitation accepted!", Toast.LENGTH_SHORT).show()
                        val repoId = wrapper.data.repositoryId ?: repositoryId
                        
                        if (repoId != null) {
                            val detailIntent = Intent(this@AcceptInviteActivity, RepositoryDetailActivity::class.java).apply {
                                putExtra("REPO_ID", repoId)
                                putExtra("REPO_NAME", repositoryName ?: "Repository")
                            }
                            startActivity(detailIntent)
                        } else {
                            startActivity(Intent(this@AcceptInviteActivity, MainActivity::class.java))
                        }
                        finish()
                    } else {
                        val errorMsg = wrapper?.error?.message ?: "Failed to accept the invitation."
                        Toast.makeText(this@AcceptInviteActivity, errorMsg, Toast.LENGTH_LONG).show()
                        btnAccept.isEnabled = true
                        btnCancel.isEnabled = true
                        tvStatusMessage.text = "Accept the invitation to join $repositoryName."
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<AcceptInviteResponse>>, t: Throwable) {
                runOnUiThread {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this@AcceptInviteActivity, "Network error accepting invitation.", Toast.LENGTH_SHORT).show()
                    btnAccept.isEnabled = true
                    btnCancel.isEnabled = true
                    tvStatusMessage.text = "Accept the invitation to join $repositoryName."
                }
            }
        })
    }

    private fun rejectInvite() {
        val inviteToken = token ?: return
        btnAccept.isEnabled = false
        btnCancel.isEnabled = false
        tvStatusMessage.text = "Rejecting invitation..."
        pbLoading.visibility = View.VISIBLE

        val inviteApi = RetrofitClient.createService<InviteApi>()
        inviteApi.rejectInvitation(inviteToken).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                runOnUiThread {
                    Toast.makeText(this@AcceptInviteActivity, "Invitation rejected.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AcceptInviteActivity, MainActivity::class.java))
                    finish()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@AcceptInviteActivity, "Invitation rejected locally.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AcceptInviteActivity, MainActivity::class.java))
                    finish()
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        tvStatusMessage.visibility = if (isLoading) View.VISIBLE else View.VISIBLE
        layoutActions.visibility = if (isLoading) View.GONE else layoutActions.visibility
    }

    private fun showError(msg: String) {
        tvStatusMessage.text = msg
        tvStatusMessage.setTextColor(resources.getColor(R.color.error_red, null))
        pbLoading.visibility = View.GONE
        layoutActions.visibility = View.GONE
    }
}
