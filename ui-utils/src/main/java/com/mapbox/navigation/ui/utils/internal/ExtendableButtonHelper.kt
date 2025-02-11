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
    private val measureShrunkWidth: () -> Int,
    private val measureExtendedWidth: (text: String) -> Int,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    var isAnimationRunning = false
        private set

    constructor(
        buttonText: TextView,
        shrunkWidth: Int,
        minExtendedWidth: Float,
    ) : this(
        buttonText,
        { shrunkWidth },
        { text ->
            (buttonText.measureTextWidth(text) + shrunkWidth)
                .coerceAtLeast(minExtendedWidth).toInt()
        },
    )

    fun showTextAndExtend(text: String, duration: Long) {
        isAnimationRunning = true
        val shrunkWidth = measureShrunkWidth()
        val extendedWidth = measureExtendedWidth(text)
        getAnimator(shrunkWidth, extendedWidth).play(
            doOnStart = {
                buttonText.text = text
                buttonText.visibility = View.VISIBLE
                mainHandler.postDelayed(
                    {
                        getAnimator(extendedWidth, shrunkWidth).play(
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
