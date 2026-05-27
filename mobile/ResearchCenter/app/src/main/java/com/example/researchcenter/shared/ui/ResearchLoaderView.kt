package com.example.researchcenter.shared.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.researchcenter.R

class ResearchLoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val spinAnimator: ObjectAnimator
    private val pulseAnimatorBox: ObjectAnimator
    private val pulseAnimatorText: ObjectAnimator

    init {
        LayoutInflater.from(context).inflate(R.layout.view_research_loader, this, true)

        val spinCircle = findViewById<View>(R.id.spinCircle)
        val centerBox = findViewById<View>(R.id.centerBox)
        val tvLabel = findViewById<TextView>(R.id.tvLabel)

        // Spin animation for dashed circle (spin_3s_linear_infinite)
        spinAnimator = ObjectAnimator.ofFloat(spinCircle, "rotation", 0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        // Pulse alpha for center box
        pulseAnimatorBox = ObjectAnimator.ofFloat(centerBox, "alpha", 1f, 0.5f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
        }

        // Pulse alpha for text
        pulseAnimatorText = ObjectAnimator.ofFloat(tvLabel, "alpha", 1f, 0.5f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        spinAnimator.start()
        pulseAnimatorBox.start()
        pulseAnimatorText.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        spinAnimator.cancel()
        pulseAnimatorBox.cancel()
        pulseAnimatorText.cancel()
    }

    fun setLabel(text: String) {
        findViewById<TextView>(R.id.tvLabel).text = text
    }
}
