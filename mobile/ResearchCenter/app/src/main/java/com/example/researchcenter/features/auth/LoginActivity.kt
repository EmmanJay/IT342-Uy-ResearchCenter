package com.example.researchcenter.features.auth

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.researchcenter.R
import com.example.researchcenter.features.main.MainActivity
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.auth.AuthSessionHelper
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.shared.model.AuthResponse
import com.example.researchcenter.shared.model.GoogleAuthRequest
import com.example.researchcenter.shared.model.LoginRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvGoRegister = findViewById<TextView>(R.id.tv_go_register)
        val btnGoogle = findViewById<Button>(R.id.btn_google_sign_in)

        intent?.getStringExtra("registered_email")?.let { etEmail.setText(it) }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.google_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var isValid = true

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Invalid email format"
                isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            } else if (password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            btnLogin.isEnabled = false
            btnLogin.text = "Signing in..."

            val authApi = RetrofitClient.createService<AuthApi>()
            val request = LoginRequest(email, password)

            authApi.login(request).enqueue(object : Callback<ApiResponse<AuthResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<AuthResponse>>,
                    response: Response<ApiResponse<AuthResponse>>
                ) {
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        runOnUiThread {
                            AuthSessionHelper.saveAuth(this@LoginActivity, wrapper.data)
                            proceedToNextScreen()
                        }
                    } else {
                        val errorMsg = wrapper?.error?.code ?: "Invalid email or password."
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                            btnLogin.isEnabled = true
                            btnLogin.text = "Sign In"
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Network error. Check your connection.", Toast.LENGTH_LONG).show()
                        btnLogin.isEnabled = true
                        btnLogin.text = "Sign In"
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
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                    return
                }
                val btnLogin = findViewById<Button>(R.id.btn_login)
                btnLogin.isEnabled = false
                btnLogin.text = "Signing in..."
                val authApi = RetrofitClient.createService<AuthApi>()
                authApi.googleAuth(GoogleAuthRequest(idToken)).enqueue(object : Callback<ApiResponse<AuthResponse>> {
                    override fun onResponse(call: Call<ApiResponse<AuthResponse>>, response: Response<ApiResponse<AuthResponse>>) {
                        val wrapper = response.body()
                        runOnUiThread {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Sign In"
                            if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                                AuthSessionHelper.saveAuth(this@LoginActivity, wrapper.data)
                                proceedToNextScreen()
                            } else {
                                Toast.makeText(this@LoginActivity, "Google sign-in failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                        runOnUiThread {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Sign In"
                            Toast.makeText(this@LoginActivity, "Network error", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proceedToNextScreen() {
        val inviteToken = intent?.getStringExtra("INVITE_TOKEN")
        val nextIntent = if (!inviteToken.isNullOrBlank()) {
            Intent(this, com.example.researchcenter.features.invite.AcceptInviteActivity::class.java).apply {
                putExtra("token", inviteToken)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(nextIntent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAffinity()
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
