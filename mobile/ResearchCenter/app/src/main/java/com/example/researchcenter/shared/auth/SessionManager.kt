package com.example.researchcenter.shared.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {

    private const val PREF_NAME = "rc_session"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NAME = "user_name"
    private const val KEY_USER_ID = "user_id"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveRefreshToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_REFRESH, token).apply()
    }
    
    fun saveUserId(context: Context, id: Long) {
        getPrefs(context).edit().putLong(KEY_USER_ID, id).apply()
    }

    fun getUserId(context: Context): Long =
        getPrefs(context).getLong(KEY_USER_ID, 0L)

    private const val KEY_ROLE = "user_role"

    fun saveEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply()
    }

    fun saveName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_NAME, name).apply()
    }

    fun saveRole(context: Context, role: String) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply()
    }

    fun getToken(context: Context): String? =
        getPrefs(context).getString(KEY_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        getPrefs(context).getString(KEY_REFRESH, null)

    fun getEmail(context: Context): String? =
        getPrefs(context).getString(KEY_EMAIL, null)

    fun getName(context: Context): String? =
        getPrefs(context).getString(KEY_NAME, null)

    fun getRole(context: Context): String? =
        getPrefs(context).getString(KEY_ROLE, null)

    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
