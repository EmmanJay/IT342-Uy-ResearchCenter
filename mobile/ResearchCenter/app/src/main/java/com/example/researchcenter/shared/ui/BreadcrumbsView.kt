package com.example.researchcenter.shared.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.researchcenter.R

class BreadcrumbsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setPath(items: List<Pair<String, () -> Unit>>) {
        removeAllViews()
        
        items.forEachIndexed { index, pair ->
            val tv = TextView(context).apply {
                text = pair.first
                textSize = 14f
                setPadding(8, 8, 8, 8)
                
                if (index == items.size - 1) {
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setOnClickListener { pair.second() }
                }
            }
            addView(tv)
            
            if (index < items.size - 1) {
                val chevron = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_media_play) // Placeholder for chevron right
                    setColorFilter(ContextCompat.getColor(context, R.color.text_hint))
                    layoutParams = LayoutParams(32, 32).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }
                addView(chevron)
            }
        }
    }
}
