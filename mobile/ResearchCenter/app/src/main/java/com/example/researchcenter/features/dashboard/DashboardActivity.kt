package com.example.researchcenter.features.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.shared.api.ApiClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.UserData

class DashboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvWelcome = findViewById<TextView>(R.id.tv_welcome)
        val tvAvatar = findViewById<TextView>(R.id.tv_avatar)
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        val token = SessionManager.getToken(this)
        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        ApiClient.getMe(token, object : ApiClient.ApiCallback<UserData> {
            override fun onSuccess(result: UserData) {
                runOnUiThread {
                    tvWelcome.text = "Welcome back, ${result.firstname}!"
                    val firstInitial = result.firstname.firstOrNull()?.toString() ?: "R"
                    val lastInitial = result.lastname.firstOrNull()?.toString() ?: "C"
                    tvAvatar.text = (firstInitial + lastInitial).uppercase()
                    SessionManager.saveName(
                        this@DashboardActivity,
                        "${result.firstname} ${result.lastname}"
                    )
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    tvWelcome.text = "Welcome back!"
                }
            }
        })

        btnLogout.setOnClickListener {
            SessionManager.clearSession(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAffinity()
    }
}
