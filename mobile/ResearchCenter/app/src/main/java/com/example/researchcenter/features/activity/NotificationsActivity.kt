package com.example.researchcenter.features.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.researchcenter.R

/**
 * Full-screen activity for Notifications (what OTHERS did in repos you belong to).
 * Opened from the bell icon in the top bar.
 */
class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Set up back button
        findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        val name = com.example.researchcenter.shared.auth.SessionManager.getName(this)
        val email = com.example.researchcenter.shared.auth.SessionManager.getEmail(this)
        findViewById<com.example.researchcenter.shared.ui.UserAvatarView>(R.id.tv_avatar)?.setUser(name, email, com.example.researchcenter.shared.auth.SessionManager.getProfilePicture(this))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
