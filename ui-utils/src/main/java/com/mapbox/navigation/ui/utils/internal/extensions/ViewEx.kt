@file:JvmName("ViewEx")

package com.mapbox.navigation.ui.utils.internal.extensions

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.slideWidth(fromWidth: Int, toWidth: Int, duration: Long): ValueAnimator {
    return ValueAnimator
        .ofInt(fromWidth, toWidth)
        .setDuration(duration).also {
            it.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                layoutParams.width = value
                requestLayout()
            }
        }
}

fun View.slideHeight(fromHeight: Int, toHeight: Int, duration: Long): ValueAnimator {
    return ValueAnimator
        .ofInt(fromHeight, toHeight)
        .setDuration(duration).also {
            it.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                layoutParams.height = value
                requestLayout()
            }
        }
}

fun ValueAnimator.play(
    doOnStart: (() -> Unit)? = null,
    doOnEnd: (() -> Unit)? = null,
) {
    val set = AnimatorSet()
    set.play(this)
    set.interpolator = AccelerateDecelerateInterpolator()
    set.addListener(
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                doOnStart?.invoke()
            }

            override fun onAnimationEnd(animation: Animator) {
                doOnEnd?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                // No implementation
            }

            override fun onAnimationRepeat(animation: Animator) {
                // No implementation
            }
        },
    )
    set.start()
}
