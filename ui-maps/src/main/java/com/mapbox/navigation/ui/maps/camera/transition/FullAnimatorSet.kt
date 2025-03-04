package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet

internal class FullAnimatorSet(
    private val animatorSet: AnimatorSet,
) : MapboxAnimatorSet {

    override val children: List<Animator> = animatorSet.childAnimations

    fun addListener(listener: MapboxAnimatorSetListener) {
        animatorSet.addListener(
            object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    listener.onAnimationStart(this@FullAnimatorSet)
                }

                override fun onAnimationEnd(animation: Animator) {
                    listener.onAnimationEnd(this@FullAnimatorSet)
                }

                override fun onAnimationCancel(animation: Animator) {
                    listener.onAnimationCancel(this@FullAnimatorSet)
                }

                override fun onAnimationRepeat(animation: Animator) {
                    // no-op
                }
            },
        )
    }

    override fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener) {
        animatorSet.addListener(
            object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    // no-op
                }

                override fun onAnimationEnd(animation: Animator) {
                    listener.onAnimationEnd(this@FullAnimatorSet)
                }

                override fun onAnimationCancel(animation: Animator) {
                    // no-op
                }

                override fun onAnimationRepeat(animation: Animator) {
                    // no-op
                }
            },
        )
    }

    override fun makeInstant() {
        animatorSet.duration = 0
    }

    override fun start() {
        animatorSet.start()
    }

    override fun end() {
        animatorSet.end()
    }

    override fun cancel() {
        animatorSet.cancel()
    }
}
