package com.example.researchcenter.features.profile

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.example.researchcenter.R
import com.example.researchcenter.shared.auth.SessionManager

class ProfileActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvName = findViewById<TextView>(R.id.tv_profile_name)
        val tvEmail = findViewById<TextView>(R.id.tv_profile_email)

        tvName.text = SessionManager.getName(this) ?: "ResearchCenter User"
        tvEmail.text = SessionManager.getEmail(this) ?: "No email"
    }
}
