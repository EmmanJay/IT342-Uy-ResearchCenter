package com.example.researchcenter.shared.auth

import android.content.Context
import com.example.researchcenter.shared.model.AuthResponse

object AuthSessionHelper {
    fun saveAuth(context: Context, data: AuthResponse) {
        SessionManager.saveToken(context, data.accessToken)
        SessionManager.saveRefreshToken(context, data.refreshToken)
        SessionManager.saveUserId(context, data.id)
        SessionManager.saveEmail(context, data.email)
        SessionManager.saveName(context, "${data.firstname} ${data.lastname}".trim())
        SessionManager.saveRole(context, data.role)
    }
}
