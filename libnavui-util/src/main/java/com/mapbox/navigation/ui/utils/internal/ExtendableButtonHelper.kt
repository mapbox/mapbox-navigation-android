package com.mapbox.navigation.ui.utils.internal

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth
import com.mapbox.navigation.ui.utils.internal.extensions.play
import com.mapbox.navigation.ui.utils.internal.extensions.slideWidth

private const val slideDuration = 300L

class ExtendableButtonHelper(
    private val buttonText: TextView,
    private val shrunkWidth: Int,
    private val minExtendedWidth: Float,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    var isAnimationRunning = false
        private set

    fun showTextAndExtend(text: String, duration: Long) {
        isAnimationRunning = true
        val extendedWidth = (buttonText.measureTextWidth(text) + shrunkWidth)
            .coerceAtLeast(minExtendedWidth)
        getAnimator(shrunkWidth, extendedWidth.toInt()).play(
            doOnStart = {
                buttonText.text = text
                buttonText.visibility = View.VISIBLE
                mainHandler.postDelayed(
                    {
                        getAnimator(extendedWidth.toInt(), shrunkWidth).play(
                            doOnStart = {
                                buttonText.text = null
                            },
                            doOnEnd = {
                                buttonText.visibility = View.INVISIBLE
                                isAnimationRunning = false
                            },
                        )
                    },
                    duration,
                )
            },
        )
    }

    fun removeDelayedAnimations() {
        mainHandler.removeCallbacksAndMessages(null)
        isAnimationRunning = false
    }

    private fun getAnimator(from: Int, to: Int) = buttonText.slideWidth(from, to, slideDuration)
}
