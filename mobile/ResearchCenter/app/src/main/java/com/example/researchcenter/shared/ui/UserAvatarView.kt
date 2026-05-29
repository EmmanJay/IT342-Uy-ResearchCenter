package com.example.researchcenter.shared.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.researchcenter.R

class UserAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tvInitials: TextView
    private val ivAvatarImage: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_user_avatar, this, true)
        tvInitials = findViewById(R.id.tvInitials)
        ivAvatarImage = findViewById(R.id.ivAvatarImage)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val card = findViewById<com.google.android.material.card.MaterialCardView>(R.id.avatarCard)
        if (card != null) {
            card.radius = (w.coerceAtMost(h) / 2).toFloat()
        }
        
        val sizeDp = w / resources.displayMetrics.density
        if (sizeDp > 60) {
            tvInitials.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28f)
        } else {
            tvInitials.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        }
    }

    fun setUser(name: String?, email: String?, imageUrl: String? = null) {
        if (!imageUrl.isNullOrEmpty()) {
            ivAvatarImage.visibility = View.VISIBLE
            tvInitials.visibility = View.GONE
            // Use Glide or Picasso here to load the image if needed.
            // For now, mirroring the default Web fallback to initials if image loading is not fully implemented.
        } else {
            ivAvatarImage.visibility = View.GONE
            tvInitials.visibility = View.VISIBLE
            tvInitials.text = getInitials(name, email)
        }
    }

    private fun getInitials(name: String?, email: String?): String {
        if (!name.isNullOrBlank()) {
            val parts = name.trim().split(" ")
            if (parts.size >= 2) {
                return (parts[0].take(1) + parts[1].take(1)).uppercase()
            }
            return name.take(2).uppercase()
        }
        if (!email.isNullOrBlank()) {
            return email.take(2).uppercase()
        }
        return "U"
    }
}
