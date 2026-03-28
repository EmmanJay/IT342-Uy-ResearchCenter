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

class RegisterActivity : Activity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFirstName = findViewById<EditText>(R.id.et_first_name)
        val etLastName = findViewById<EditText>(R.id.et_last_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvGoLogin = findViewById<TextView>(R.id.tv_go_login)
        val ivToggle = findViewById<ImageView>(R.id.iv_toggle_password)
        val progressRegister = findViewById<ProgressBar>(R.id.progress_register)
        val btnGoogle = findViewById<Button>(R.id.btn_google_sign_up)

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

        btnRegister.setOnClickListener {
            tvError.visibility = View.GONE

            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                tvError.text = "All fields are required"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvError.text = "Please enter a valid email address"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 8) {
                tvError.text = "Password must be at least 8 characters"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password != confirm) {
                tvError.text = "Passwords do not match"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            progressRegister.visibility = View.VISIBLE

            ApiClient.register(
                email,
                password,
                firstName,
                lastName,
                object : ApiClient.ApiCallback<LoginResponse> {
                    override fun onSuccess(result: LoginResponse) {
                        runOnUiThread {
                            SessionManager.saveToken(this@RegisterActivity, result.token)
                            SessionManager.saveRefreshToken(this@RegisterActivity, result.refreshToken)
                            SessionManager.saveEmail(this@RegisterActivity, email)
                            SessionManager.saveName(this@RegisterActivity, "$firstName $lastName")
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            intent.putExtra("registered_email", email)
                            startActivity(intent)
                            finish()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
                            // Show detailed error message
                            tvError.text = when {
                                error == "AUTH-002" -> "An account with this email already exists."
                                error.contains("Network error") -> "Network error. Check your connection."
                                error.contains("Parse error") -> "Response parsing error. Please try again."
                                error.contains("Empty response") -> "No response from server."
                                error.startsWith("Error") -> "Server error: $error"
                                else -> error
                            }
                            tvError.visibility = View.VISIBLE
                            Toast.makeText(this@RegisterActivity, error, Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            progressRegister.visibility = View.GONE
                        }
                    }
                }
            )
        }

        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }

        tvGoLogin.setOnClickListener {
            finish()
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
                Toast.makeText(this, "Google Sign-Up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
