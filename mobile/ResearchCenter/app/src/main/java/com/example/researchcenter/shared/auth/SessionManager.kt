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
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(PREF_NAME)
                } else {
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
                    val file = java.io.File(context.filesDir.parent, "shared_prefs/$PREF_NAME.xml")
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

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
    private const val KEY_PROFILE_PIC = "user_profile_pic"

    fun saveEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply()
    }

    fun saveName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_NAME, name).apply()
    }

    fun saveRole(context: Context, role: String) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply()
    }

    fun saveProfilePicture(context: Context, url: String?) {
        getPrefs(context).edit().putString(KEY_PROFILE_PIC, url).apply()
    }

    fun getProfilePicture(context: Context): String? =
        getPrefs(context).getString(KEY_PROFILE_PIC, null)

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

    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        val parts = token.split(".")
        if (parts.size < 2) return true
        return try {
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT))
            val json = org.json.JSONObject(payload)
            val exp = json.optLong("exp", 0L)
            val expMs = exp * 1000
            val currentMs = System.currentTimeMillis()
            currentMs >= expMs
        } catch (e: Exception) {
            true // Treat parse failure as expired
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        val token = getToken(context) ?: return false
        if (isTokenExpired(token)) {
            clearSession(context)
            return false
        }
        return true
    }

    private const val KEY_LOCAL_AVATAR = "local_avatar_uri"

    fun saveLocalAvatarUri(context: Context, uri: String?) {
        getPrefs(context).edit().putString(KEY_LOCAL_AVATAR, uri).apply()
    }

    fun getLocalAvatarUri(context: Context): String? =
        getPrefs(context).getString(KEY_LOCAL_AVATAR, null)

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
