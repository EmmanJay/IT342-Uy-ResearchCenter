package com.example.researchcenter

import android.app.Application
import com.example.researchcenter.shared.api.ApiClient
import com.example.researchcenter.shared.auth.SessionManager

class ResearchCenterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.appContext = this
        ApiClient.init { SessionManager.getToken(this) }
    }
}
