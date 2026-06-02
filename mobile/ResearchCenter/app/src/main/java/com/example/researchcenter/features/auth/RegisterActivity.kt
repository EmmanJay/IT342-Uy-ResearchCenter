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
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.ApiResponse
import com.example.researchcenter.features.dashboard.DashboardActivity
import com.example.researchcenter.shared.auth.AuthSessionHelper
import com.example.researchcenter.shared.model.AuthResponse
import com.example.researchcenter.shared.model.GoogleAuthRequest
import com.example.researchcenter.shared.model.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val tilFirstName = findViewById<TextInputLayout>(R.id.til_firstname)
        val tilLastName = findViewById<TextInputLayout>(R.id.til_lastname)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_confirm_password)

        val etFirstName = findViewById<TextInputEditText>(R.id.et_firstname)
        val etLastName = findViewById<TextInputEditText>(R.id.et_lastname)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvGoLogin = findViewById<TextView>(R.id.tv_go_login)
        val btnGoogle = findViewById<Button>(R.id.btn_google_sign_up)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.google_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnRegister.setOnClickListener {
            tilFirstName.error = null
            tilLastName.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            var isValid = true

            if (firstName.isEmpty()) {
                tilFirstName.error = "Required"
                isValid = false
            }
            if (lastName.isEmpty()) {
                tilLastName.error = "Required"
                isValid = false
            }
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
                tilPassword.error = "Must be at least 8 characters"
                isValid = false
            }
            if (password != confirm) {
                tilConfirmPassword.error = "Passwords do not match"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            btnRegister.isEnabled = false
            btnRegister.text = "Creating Account..."

            val authApi = RetrofitClient.createService<AuthApi>()
            val request = RegisterRequest(email, password, firstName, lastName)

            authApi.register(request).enqueue(object : Callback<ApiResponse<AuthResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<AuthResponse>>,
                    response: Response<ApiResponse<AuthResponse>>
                ) {
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        runOnUiThread {
                            AuthSessionHelper.saveAuth(this@RegisterActivity, wrapper.data)
                            startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                            finish()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    } else {
                        var errorMsg = "An account with this email already exists."
                        try {
                            val errorStr = response.errorBody()?.string()
                            if (!errorStr.isNullOrBlank()) {
                                if (errorStr.trim().startsWith("{")) {
                                    val errorObj = com.google.gson.Gson().fromJson(errorStr, ApiResponse::class.java)
                                    errorMsg = errorObj.error?.message ?: errorObj.error?.code ?: errorMsg
                                } else {
                                    errorMsg = "Server Error ${response.code()}: ${errorStr.take(30)}"
                                }
                            } else {
                                errorMsg = "Server Error ${response.code()}"
                            }
                        } catch (e: Exception) {
                            errorMsg = "Server Error ${response.code()}"
                        }
                        
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            btnRegister.text = "Create Account"
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Network error. Check your connection.", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account"
                    }
                }
            })
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
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(this, "Google Sign-Up failed", Toast.LENGTH_SHORT).show()
                    return
                }
                val btnRegister = findViewById<Button>(R.id.btn_register)
                btnRegister.isEnabled = false
                btnRegister.text = "Creating Account..."
                val authApi = RetrofitClient.createService<AuthApi>()
                authApi.googleAuth(GoogleAuthRequest(idToken)).enqueue(object : Callback<ApiResponse<AuthResponse>> {
                    override fun onResponse(call: Call<ApiResponse<AuthResponse>>, response: Response<ApiResponse<AuthResponse>>) {
                        val wrapper = response.body()
                        runOnUiThread {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Create Account"
                            if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                                AuthSessionHelper.saveAuth(this@RegisterActivity, wrapper.data)
                                startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@RegisterActivity, "Google sign-up failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                        runOnUiThread {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Create Account"
                            Toast.makeText(this@RegisterActivity, "Network error", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-Up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
