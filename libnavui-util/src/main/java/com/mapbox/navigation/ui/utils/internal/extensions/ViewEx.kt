@file:JvmName("ViewEx")

package com.mapbox.navigation.ui.utils.internal.extensions

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator

fun View.afterMeasured(f: View.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

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

fun View.extend(
    animator: ValueAnimator,
    doOnEnd: () -> Unit
) {
    val set = AnimatorSet()
    set.play(animator)
    set.interpolator = AccelerateDecelerateInterpolator()
    set.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            // No implementation
        }

        override fun onAnimationEnd(animation: Animator?) {
            doOnEnd()
        }

        override fun onAnimationCancel(animation: Animator?) {
            // No implementation
        }

        override fun onAnimationRepeat(animation: Animator?) {
            // No implementation
        }
    })
    set.start()
}

fun View.shrink(
    animator: ValueAnimator,
    doOnStart: () -> Unit
) {
    val set = AnimatorSet()
    set.play(animator)
    set.interpolator = AccelerateDecelerateInterpolator()
    set.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            doOnStart()
        }

        override fun onAnimationEnd(animation: Animator?) {
            // No implementation
        }

        override fun onAnimationCancel(animation: Animator?) {
            // No implementation
        }

        override fun onAnimationRepeat(animation: Animator?) {
            // No implementation
        }
    })
    set.start()
}
