# ResearchCenter — Mobile Development Guide (Phase 2)

> Android app built with Kotlin + XML Layouts  
> Connects to the existing Phase 1 Spring Boot backend  
> Place all mobile code inside the `mobile/` folder of the same repo

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts (NO Jetpack Compose) |
| HTTP Client | OkHttp (manual, no Retrofit) |
| Local Storage | SharedPreferences via SessionManager |
| Auth | JWT Bearer Token + Google Sign-In |
| Min SDK | API 24 (Android 7.0+) |
| Build Tool | Gradle |

---

## Package Name

```
com.example.researchcenterlabact
```

---

## Exact File Structure

```
mobile/ResearchCenterLabAct/
└── app/
    └── src/
        ├── androidTest/java/com/example/researchcenterlabact/
        │   └── ExampleInstrumentedTest.kt
        └── main/
            ├── java/com/example/researchcenterlabact/
            │   ├── api/
            │   │   └── ApiClient.kt
            │   ├── auth/
            │   │   └── SessionManager.kt
            │   ├── model/
            │   │   ├── AuthModels.kt
            │   │   └── User.kt
            │   ├── DashboardActivity.kt
            │   ├── LoginActivity.kt
            │   ├── ProfileActivity.kt
            │   ├── RegisterActivity.kt
            │   └── SplashActivity.kt
            └── res/
                ├── drawable/
                ├── layout/
                │   ├── activity_splash.xml
                │   ├── activity_login.xml
                │   ├── activity_register.xml
                │   ├── activity_dashboard.xml
                │   └── activity_profile.xml
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                └── xml/
```

---

## Design System

### Colors (`res/values/colors.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary_green">#2E7D32</color>
    <color name="primary_green_dark">#1B5E20</color>
    <color name="secondary_green">#4CAF50</color>
    <color name="error_red">#D32F2F</color>
    <color name="background">#F5F5F5</color>
    <color name="surface">#FFFFFF</color>
    <color name="text_primary">#212121</color>
    <color name="text_secondary">#757575</color>
    <color name="border">#E0E0E0</color>
    <color name="light_green_bg">#E8F5E9</color>
</resources>
```

---

## Gradle Dependencies (`app/build.gradle`)

```groovy
android {
    defaultConfig {
        minSdk 24
    }
}

dependencies {
    // OkHttp for API calls
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Gson for JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'

    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:20.7.0'

    // Material Design (for styled inputs/buttons)
    implementation 'com.google.android.material:material:1.11.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

Project-level `build.gradle`:
```groovy
plugins {
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

---

## Data Models

### `model/AuthModels.kt`
```kotlin
package com.example.researchcenterlabact.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstname: String,
    val lastname: String
)

data class LoginResponse(
    val token: String,           // accessToken from backend
    val refreshToken: String
)

data class RegisterResponse(
    val token: String,
    val refreshToken: String
)

// Standard SDD wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val timestamp: String
)

data class ApiError(
    val code: String,
    val message: String
)

data class AuthData(
    val user: UserData,
    val accessToken: String,
    val refreshToken: String
)

data class UserData(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String
)
```

### `model/User.kt`
```kotlin
package com.example.researchcenterlabact.model

data class User(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    val createdAt: String?
)
```

---

## SessionManager (`auth/SessionManager.kt`)

```kotlin
package com.example.researchcenterlabact.auth

import android.content.Context

object SessionManager {

    private const val PREF_NAME    = "rc_session"
    private const val KEY_TOKEN    = "access_token"
    private const val KEY_REFRESH  = "refresh_token"
    private const val KEY_EMAIL    = "user_email"
    private const val KEY_NAME     = "user_name"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveRefreshToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_REFRESH, token).apply()
    }

    fun saveEmail(context: Context, email: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_EMAIL, email).apply()
    }

    fun saveName(context: Context, name: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    fun getEmail(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null)

    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
```

---

## ApiClient (`api/ApiClient.kt`)

```kotlin
package com.example.researchcenterlabact.api

import com.example.researchcenterlabact.model.*
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object ApiClient {

    // Use 10.0.2.2 for emulator, replace with your IP for real device
    private const val BASE_URL = "http://10.0.2.2:8080/api/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }

    // ── Register ──────────────────────────────────────────────────────────
    fun register(
        email: String,
        password: String,
        firstname: String,
        lastname: String,
        callback: ApiCallback<LoginResponse>
    ) {
        val body = gson.toJson(
            mapOf(
                "email"     to email,
                "password"  to password,
                "firstname" to firstname,
                "lastname"  to lastname
            )
        ).toRequestBody(JSON)

        val request = Request.Builder()
            .url("$BASE_URL/auth/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val wrapper = gson.fromJson(
                            responseBody,
                            ApiResponse::class.java
                        )
                        if (wrapper.success == true) {
                            val dataMap = wrapper.data as? Map<*, *>
                            val accessToken  = dataMap?.get("accessToken")  as? String ?: ""
                            val refreshToken = dataMap?.get("refreshToken") as? String ?: ""
                            callback.onSuccess(LoginResponse(accessToken, refreshToken))
                        } else {
                            val code = (wrapper.error as? Map<*, *>)?.get("code") as? String
                            callback.onError(code ?: "Registration failed")
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("Error ${response.code}")
                }
            }
        })
    }

    // ── Login ─────────────────────────────────────────────────────────────
    fun login(
        email: String,
        password: String,
        callback: ApiCallback<LoginResponse>
    ) {
        val body = gson.toJson(
            mapOf("email" to email, "password" to password)
        ).toRequestBody(JSON)

        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val wrapper = gson.fromJson(
                            responseBody,
                            ApiResponse::class.java
                        )
                        if (wrapper.success == true) {
                            val dataMap = wrapper.data as? Map<*, *>
                            val accessToken  = dataMap?.get("accessToken")  as? String ?: ""
                            val refreshToken = dataMap?.get("refreshToken") as? String ?: ""
                            callback.onSuccess(LoginResponse(accessToken, refreshToken))
                        } else {
                            val code = (wrapper.error as? Map<*, *>)?.get("code") as? String
                            callback.onError(code ?: "AUTH-001")
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("AUTH-001")
                }
            }
        })
    }

    // ── Get Current User (/me) ─────────────────────────────────────────────
    fun getMe(
        token: String,
        callback: ApiCallback<UserData>
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/users/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val wrapper = gson.fromJson(responseBody, ApiResponse::class.java)
                        if (wrapper.success == true) {
                            val dataMap = wrapper.data as? Map<*, *>
                            val user = UserData(
                                id        = (dataMap?.get("id") as? Double)?.toLong() ?: 0L,
                                email     = dataMap?.get("email")     as? String ?: "",
                                firstname = dataMap?.get("firstname") as? String ?: "",
                                lastname  = dataMap?.get("lastname")  as? String ?: "",
                                role      = dataMap?.get("role")      as? String ?: ""
                            )
                            callback.onSuccess(user)
                        } else {
                            callback.onError("Failed to get user")
                        }
                    } catch (e: Exception) {
                        callback.onError("Parse error: ${e.message}")
                    }
                } else {
                    callback.onError("Error ${response.code}")
                }
            }
        })
    }
}
```

---

## SplashActivity.kt

```kotlin
package com.example.researchcenterlabact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.researchcenterlabact.auth.SessionManager

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            if (SessionManager.isLoggedIn(this)) {
                startActivity(Intent(this, DashboardActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1500)
    }
}
```

### `activity_splash.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="@color/primary_green">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="ResearchCenter"
        android:textColor="@color/surface"
        android:textSize="28sp"
        android:textStyle="bold"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Collaborative Research Platform"
        android:textColor="#B9F6CA"
        android:textSize="14sp"
        android:layout_marginTop="8dp"/>

    <ProgressBar
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="40dp"
        android:indeterminateTint="@color/surface"/>

</LinearLayout>
```

---

## LoginActivity.kt

```kotlin
package com.example.researchcenterlabact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import com.example.researchcenterlabact.api.ApiClient
import com.example.researchcenterlabact.auth.SessionManager
import com.example.researchcenterlabact.model.LoginResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : Activity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail        = findViewById<EditText>(R.id.et_email)
        val etPassword     = findViewById<EditText>(R.id.et_password)
        val btnLogin       = findViewById<Button>(R.id.btn_login)
        val tvError        = findViewById<TextView>(R.id.tv_error)
        val tvGoRegister   = findViewById<TextView>(R.id.tv_go_register)
        val ivToggle       = findViewById<ImageView>(R.id.iv_toggle_password)
        val progressLogin  = findViewById<ProgressBar>(R.id.progress_login)
        val btnGoogle      = findViewById<Button>(R.id.btn_google_sign_in)

        // Auto-fill email if coming from registration
        intent?.getStringExtra("registered_email")?.let { etEmail.setText(it) }

        // Setup Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Password toggle
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

        // Login
        btnLogin.setOnClickListener {
            tvError.visibility = View.GONE
            val email    = etEmail.text.toString().trim()
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
                        SessionManager.saveEmail(this@LoginActivity, email)
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        tvError.text = if (error == "AUTH-001") "Invalid email or password."
                            else "Login failed. Please try again."
                        tvError.visibility = View.VISIBLE
                        btnLogin.isEnabled = true
                        progressLogin.visibility = View.GONE
                    }
                }
            })
        }

        // Google Sign-In
        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }

        // Go to Register
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
                // TODO: send account.idToken to backend /api/v1/auth/google
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
```

### `activity_login.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <!-- Logo -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="ResearchCenter"
            android:textColor="@color/primary_green"
            android:textSize="22sp"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginTop="40dp"
            android:layout_marginBottom="32dp"/>

        <!-- Card -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="24dp"
            android:background="@color/surface"
            android:elevation="4dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Sign In"
                android:textColor="@color/text_primary"
                android:textSize="22sp"
                android:textStyle="bold"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Enter your credentials to access your account"
                android:textColor="@color/text_secondary"
                android:textSize="13sp"
                android:layout_marginTop="4dp"
                android:layout_marginBottom="20dp"/>

            <!-- Email -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Email Address"
                android:textColor="@color/text_primary"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginBottom="4dp"/>

            <EditText
                android:id="@+id/et_email"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:inputType="textEmailAddress"
                android:hint="you@example.com"
                android:textSize="14sp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:background="@drawable/bg_input"
                android:layout_marginBottom="16dp"/>

            <!-- Password -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Password"
                android:textColor="@color/text_primary"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginBottom="4dp"/>

            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginBottom="8dp">

                <EditText
                    android:id="@+id/et_password"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:inputType="textPassword"
                    android:hint="••••••••"
                    android:textSize="14sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="48dp"
                    android:background="@drawable/bg_input"/>

                <ImageView
                    android:id="@+id/iv_toggle_password"
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:layout_gravity="center_vertical|end"
                    android:layout_marginEnd="12dp"
                    android:src="@drawable/ic_eye_closed"
                    android:contentDescription="Toggle password visibility"/>
            </FrameLayout>

            <!-- Forgot Password -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Forgot Password?"
                android:textColor="@color/primary_green"
                android:textSize="13sp"
                android:gravity="end"
                android:layout_marginBottom="16dp"/>

            <!-- Error -->
            <TextView
                android:id="@+id/tv_error"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/error_red"
                android:textSize="13sp"
                android:layout_marginBottom="8dp"
                android:visibility="gone"/>

            <!-- Sign In Button -->
            <Button
                android:id="@+id/btn_login"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Sign In"
                android:textColor="@color/surface"
                android:backgroundTint="@color/primary_green"
                android:textSize="14sp"
                android:textStyle="bold"/>

            <ProgressBar
                android:id="@+id/progress_login"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="8dp"
                android:visibility="gone"/>

            <!-- Divider -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="16dp">
                <View android:layout_width="0dp" android:layout_height="1dp"
                    android:layout_weight="1" android:background="@color/border"/>
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="or" android:textColor="@color/text_secondary"
                    android:textSize="13sp" android:paddingStart="12dp" android:paddingEnd="12dp"/>
                <View android:layout_width="0dp" android:layout_height="1dp"
                    android:layout_weight="1" android:background="@color/border"/>
            </LinearLayout>

            <!-- Google -->
            <Button
                android:id="@+id/btn_google_sign_in"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Sign in with Google"
                android:textColor="@color/text_primary"
                android:backgroundTint="@color/surface"
                android:textSize="14sp"/>

            <!-- Sign Up Link -->
            <TextView
                android:id="@+id/tv_go_register"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="16dp"
                android:textSize="13sp"
                android:textColor="@color/text_secondary"
                android:text="Don\'t have an account? Sign Up"/>

        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

---

## RegisterActivity.kt

```kotlin
package com.example.researchcenterlabact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import com.example.researchcenterlabact.api.ApiClient
import com.example.researchcenterlabact.auth.SessionManager
import com.example.researchcenterlabact.model.LoginResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class RegisterActivity : Activity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFirstName       = findViewById<EditText>(R.id.et_first_name)
        val etLastName        = findViewById<EditText>(R.id.et_last_name)
        val etEmail           = findViewById<EditText>(R.id.et_email)
        val etPassword        = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnRegister       = findViewById<Button>(R.id.btn_register)
        val tvError           = findViewById<TextView>(R.id.tv_error)
        val tvGoLogin         = findViewById<TextView>(R.id.tv_go_login)
        val ivToggle          = findViewById<ImageView>(R.id.iv_toggle_password)
        val progressRegister  = findViewById<ProgressBar>(R.id.progress_register)
        val btnGoogle         = findViewById<Button>(R.id.btn_google_sign_up)

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Password toggle
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

        // Register
        btnRegister.setOnClickListener {
            tvError.visibility = View.GONE

            val firstName = etFirstName.text.toString().trim()
            val lastName  = etLastName.text.toString().trim()
            val email     = etEmail.text.toString().trim()
            val password  = etPassword.text.toString().trim()
            val confirm   = etConfirmPassword.text.toString().trim()

            // Validation
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
                email, password, firstName, lastName,
                object : ApiClient.ApiCallback<LoginResponse> {
                    override fun onSuccess(result: LoginResponse) {
                        runOnUiThread {
                            SessionManager.saveToken(this@RegisterActivity, result.token)
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
                            tvError.text = if (error == "AUTH-002")
                                "An account with this email already exists."
                            else "Registration failed. Please try again."
                            tvError.visibility = View.VISIBLE
                            btnRegister.isEnabled = true
                            progressRegister.visibility = View.GONE
                        }
                    }
                }
            )
        }

        // Google Sign-Up
        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }

        // Go to Login
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
                // TODO: send account.idToken to backend /api/v1/auth/google
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
```

### `activity_register.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="ResearchCenter"
            android:textColor="@color/primary_green"
            android:textSize="22sp"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginTop="40dp"
            android:layout_marginBottom="32dp"/>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="24dp"
            android:background="@color/surface"
            android:elevation="4dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Create Account"
                android:textColor="@color/text_primary"
                android:textSize="22sp"
                android:textStyle="bold"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Join ResearchCenter to collaborate on research"
                android:textColor="@color/text_secondary"
                android:textSize="13sp"
                android:layout_marginTop="4dp"
                android:layout_marginBottom="20dp"/>

            <!-- First + Last Name row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginEnd="8dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="First Name"
                        android:textColor="@color/text_primary"
                        android:textSize="13sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="4dp"/>

                    <EditText
                        android:id="@+id/et_first_name"
                        android:layout_width="match_parent"
                        android:layout_height="48dp"
                        android:inputType="textPersonName"
                        android:hint="John"
                        android:textSize="14sp"
                        android:paddingStart="12dp"
                        android:paddingEnd="12dp"
                        android:background="@drawable/bg_input"/>
                </LinearLayout>

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="8dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Last Name"
                        android:textColor="@color/text_primary"
                        android:textSize="13sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="4dp"/>

                    <EditText
                        android:id="@+id/et_last_name"
                        android:layout_width="match_parent"
                        android:layout_height="48dp"
                        android:inputType="textPersonName"
                        android:hint="Doe"
                        android:textSize="14sp"
                        android:paddingStart="12dp"
                        android:paddingEnd="12dp"
                        android:background="@drawable/bg_input"/>
                </LinearLayout>
            </LinearLayout>

            <!-- Email -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Email Address"
                android:textColor="@color/text_primary"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginBottom="4dp"/>

            <EditText
                android:id="@+id/et_email"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:inputType="textEmailAddress"
                android:hint="you@example.com"
                android:textSize="14sp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:background="@drawable/bg_input"
                android:layout_marginBottom="16dp"/>

            <!-- Password -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Password"
                android:textColor="@color/text_primary"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginBottom="4dp"/>

            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="48dp">

                <EditText
                    android:id="@+id/et_password"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:inputType="textPassword"
                    android:hint="••••••••"
                    android:textSize="14sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="48dp"
                    android:background="@drawable/bg_input"/>

                <ImageView
                    android:id="@+id/iv_toggle_password"
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:layout_gravity="center_vertical|end"
                    android:layout_marginEnd="12dp"
                    android:src="@drawable/ic_eye_closed"
                    android:contentDescription="Toggle password"/>
            </FrameLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="At least 8 characters"
                android:textColor="@color/text_secondary"
                android:textSize="11sp"
                android:layout_marginTop="4dp"
                android:layout_marginBottom="16dp"/>

            <!-- Confirm Password -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Confirm Password"
                android:textColor="@color/text_primary"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginBottom="4dp"/>

            <EditText
                android:id="@+id/et_confirm_password"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:inputType="textPassword"
                android:hint="••••••••"
                android:textSize="14sp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:background="@drawable/bg_input"
                android:layout_marginBottom="16dp"/>

            <!-- Error -->
            <TextView
                android:id="@+id/tv_error"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/error_red"
                android:textSize="13sp"
                android:layout_marginBottom="8dp"
                android:visibility="gone"/>

            <!-- Create Account -->
            <Button
                android:id="@+id/btn_register"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Create Account"
                android:textColor="@color/surface"
                android:backgroundTint="@color/primary_green"
                android:textSize="14sp"
                android:textStyle="bold"/>

            <ProgressBar
                android:id="@+id/progress_register"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="8dp"
                android:visibility="gone"/>

            <!-- Divider -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="16dp">
                <View android:layout_width="0dp" android:layout_height="1dp"
                    android:layout_weight="1" android:background="@color/border"/>
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="or" android:textColor="@color/text_secondary"
                    android:textSize="13sp" android:paddingStart="12dp" android:paddingEnd="12dp"/>
                <View android:layout_width="0dp" android:layout_height="1dp"
                    android:layout_weight="1" android:background="@color/border"/>
            </LinearLayout>

            <Button
                android:id="@+id/btn_google_sign_up"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Sign up with Google"
                android:textColor="@color/text_primary"
                android:backgroundTint="@color/surface"
                android:textSize="14sp"/>

            <TextView
                android:id="@+id/tv_go_login"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="16dp"
                android:textSize="13sp"
                android:textColor="@color/text_secondary"
                android:text="Already have an account? Sign In"/>

        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

---

## DashboardActivity.kt

```kotlin
package com.example.researchcenterlabact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.researchcenterlabact.api.ApiClient
import com.example.researchcenterlabact.auth.SessionManager
import com.example.researchcenterlabact.model.UserData

class DashboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvWelcome = findViewById<TextView>(R.id.tv_welcome)
        val tvAvatar  = findViewById<TextView>(R.id.tv_avatar)
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        val token = SessionManager.getToken(this)
        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load user info from /users/me
        ApiClient.getMe(token, object : ApiClient.ApiCallback<UserData> {
            override fun onSuccess(result: UserData) {
                runOnUiThread {
                    tvWelcome.text = "Welcome back, ${result.firstname}!"
                    tvAvatar.text  = "${result.firstname[0]}${result.lastname[0]}".uppercase()
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
```

### `activity_dashboard.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:background="@color/surface"
        android:elevation="4dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="ResearchCenter"
            android:textColor="@color/primary_green"
            android:textSize="18sp"
            android:textStyle="bold"/>

        <TextView
            android:id="@+id/tv_avatar"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:gravity="center"
            android:textColor="@color/surface"
            android:textSize="14sp"
            android:textStyle="bold"
            android:background="@drawable/bg_avatar"
            android:text="RC"/>
    </LinearLayout>

    <!-- Welcome -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:id="@+id/tv_welcome"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Welcome back!"
            android:textColor="@color/text_primary"
            android:textSize="22sp"
            android:textStyle="bold"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Manage your research repositories and collaborate with your team"
            android:textColor="@color/text_secondary"
            android:textSize="13sp"
            android:layout_marginTop="4dp"/>
    </LinearLayout>

    <!-- Logout -->
    <Button
        android:id="@+id/btn_logout"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:layout_marginTop="8dp"
        android:text="Logout"
        android:backgroundTint="#D32F2F"
        android:textColor="@color/surface"
        android:textStyle="bold"/>

</LinearLayout>
```

---

## Drawable: `bg_input.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <stroke android:width="1dp" android:color="#E0E0E0"/>
    <corners android:radius="8dp"/>
    <solid android:color="#FFFFFF"/>
</shape>
```

## Drawable: `bg_avatar.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#2E7D32"/>
</shape>
```

---

## AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET"/>

    <application
        android:allowBackup="true"
        android:label="ResearchCenter"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">

        <activity android:name=".SplashActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <activity android:name=".LoginActivity"    android:exported="false"/>
        <activity android:name=".RegisterActivity" android:exported="false"/>
        <activity android:name=".DashboardActivity" android:exported="false"/>
        <activity android:name=".ProfileActivity"  android:exported="false"/>

    </application>
</manifest>
```

---

## Google Sign-In Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create OAuth 2.0 credentials → Android client + Web client
3. Download `google-services.json` → place in `mobile/app/`
4. Add to `app/build.gradle`: `apply plugin: 'com.google.gms.google-services'`
5. Add `default_web_client_id` to `res/values/strings.xml`:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```
6. Add `google-services.json` to `.gitignore`

---

## API Endpoints Used (Phase 2)

| Method | Endpoint | Used In |
|---|---|---|
| `POST` | `/api/v1/auth/register` | RegisterActivity |
| `POST` | `/api/v1/auth/login` | LoginActivity |
| `GET` | `/api/v1/users/me` | DashboardActivity |

---

## Error Handling

| Error Code | Message Shown |
|---|---|
| `AUTH-001` | "Invalid email or password." |
| `AUTH-002` | "An account with this email already exists." |
| Network fail | "Network error. Please check your connection." |

---

## Final Commit Message

```
IT342 Phase 2 – Mobile Development Completed
```

---

## Notes for Opus Copilot

- Package is `com.example.researchcenterlabact` — use this exactly everywhere
- All Activities extend `Activity` not `AppCompatActivity`
- Use `findViewById` — no view binding or data binding
- All API calls use OkHttp with manual `ApiCallback` interface — no Retrofit, no coroutines
- Use `runOnUiThread {}` for all UI updates inside callbacks
- `SessionManager` uses plain `SharedPreferences` — not `EncryptedSharedPreferences`
- `10.0.2.2` maps to localhost on the Android emulator
- `SplashActivity` is the launcher — it checks session and redirects
- `finishAffinity()` prevents back navigation to auth screens after login
- `google-services.json` must be in `mobile/app/` — never commit it
- Add eye open/closed drawable icons to `res/drawable/` manually or use vector assets
- `bg_input.xml` and `bg_avatar.xml` must be created in `res/drawable/`
- `ProfileActivity` is scaffolded but not implemented in Phase 2
