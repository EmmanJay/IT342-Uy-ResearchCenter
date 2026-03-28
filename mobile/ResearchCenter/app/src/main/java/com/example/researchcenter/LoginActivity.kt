package com.example.researchcenter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.researchcenter.api.ApiClient
import com.example.researchcenter.auth.SessionManager
import com.example.researchcenter.model.LoginResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : Activity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvGoRegister = findViewById<TextView>(R.id.tv_go_register)
        val ivToggle = findViewById<ImageView>(R.id.iv_toggle_password)
        val progressLogin = findViewById<ProgressBar>(R.id.progress_login)
        val btnGoogle = findViewById<Button>(R.id.btn_google_sign_in)

        intent?.getStringExtra("registered_email")?.let { etEmail.setText(it) }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.google_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        var isPasswordVisible = false
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        ivToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible) null
            else PasswordTransformationMethod.getInstance()
            ivToggle.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            tvError.visibility = View.GONE
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill in both email and password"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            progressLogin.visibility = View.VISIBLE

            ApiClient.login(email, password, object : ApiClient.ApiCallback<LoginResponse> {
                override fun onSuccess(result: LoginResponse) {
                    runOnUiThread {
                        SessionManager.saveToken(this@LoginActivity, result.token)
                        SessionManager.saveRefreshToken(this@LoginActivity, result.refreshToken)
                        SessionManager.saveEmail(this@LoginActivity, email)
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        tvError.text = when {
                            error == "AUTH-001" -> "Invalid email or password."
                            error.contains("Network error") -> "Network error. Check your connection."
                            error.contains("Parse error") -> "Response parsing error. Please try again."
                            error.contains("Empty response") -> "No response from server."
                            error.startsWith("Error") -> "Server error: $error"
                            else -> error
                        }
                        tvError.visibility = View.VISIBLE
                        Toast.makeText(this@LoginActivity, error, Toast.LENGTH_LONG).show()
                        btnLogin.isEnabled = true
                        progressLogin.visibility = View.GONE
                    }
                }
            })
        }

        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Toast.makeText(this, "Google: ${account.email}", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAffinity()
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
