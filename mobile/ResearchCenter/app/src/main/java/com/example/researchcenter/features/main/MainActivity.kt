package com.example.researchcenter.features.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.features.bookmarks.GlobalBookmarksFragment
import com.example.researchcenter.features.activity.NotificationsFragment
import com.example.researchcenter.features.dashboard.DashboardFragment
import com.example.researchcenter.features.profile.ProfileFragment
import com.example.researchcenter.shared.api.NotificationWebSocketClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.ui.UserAvatarView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var activeFragment: Fragment? = null

    private val dashboardFragment by lazy { DashboardFragment() }
    private val bookmarksFragment by lazy { GlobalBookmarksFragment() }
    private val notificationsFragment by lazy { NotificationsFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        setupTopBar()
        setupBottomNavigation()
        connectWebSocket()

        if (savedInstanceState == null) {
            loadFragment(dashboardFragment)
            bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun setupTopBar() {
        findViewById<ImageButton>(R.id.btn_notification_bell).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_notifications
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { loadFragment(dashboardFragment); true }
                R.id.nav_notifications -> { loadFragment(notificationsFragment); true }
                R.id.nav_profile -> { loadFragment(profileFragment); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (activeFragment === fragment) return
        activeFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun refreshAvatar() {
        // No top-bar avatar to refresh
    }

    override fun onResume() {
        super.onResume()
        refreshAvatar()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationWebSocketClient.removeListener(wsListener)
    }

    private val wsListener = object : NotificationWebSocketClient.NotificationListener {
        override fun onNotificationReceived(type: String, message: String, data: JSONObject?) {
            runOnUiThread {
                if (message.isNotBlank()) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onConnectionStateChanged(connected: Boolean) {}
    }

    private fun connectWebSocket() {
        val token = SessionManager.getToken(this) ?: return
        NotificationWebSocketClient.addListener(wsListener)
        NotificationWebSocketClient.connect(token)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (bottomNav.selectedItemId != R.id.nav_dashboard) {
            bottomNav.selectedItemId = R.id.nav_dashboard
        } else {
            super.onBackPressed()
        }
    }
}
