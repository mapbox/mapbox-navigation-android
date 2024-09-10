package com.mapbox.navigation.ui.components.status.internal.extensions

import android.animation.Animator

/**
 * Add an action which will be invoked when the animation has ended without cancellation.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 */
internal inline fun Animator.doOnFinish(crossinline action: (animator: Animator) -> Unit) =
    object : Animator.AnimatorListener {
        private var cancelled = false

        override fun onAnimationStart(animator: Animator) = Unit

        override fun onAnimationRepeat(animator: Animator) = Unit

        override fun onAnimationCancel(animator: Animator) {
            cancelled = true
        }

        override fun onAnimationEnd(animator: Animator) {
            if (!cancelled) action(animator)
        }
    }.also {
        addListener(it)
    }
