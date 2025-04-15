package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin

internal class FullAnimatorSet(
    private val cameraPlugin: CameraAnimationsPlugin,
    private val animatorSet: AnimatorSet,
) : MapboxAnimatorSet {

    private val children = animatorSet.childAnimations.map { it as ValueAnimator }.toTypedArray()

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
        cameraPlugin.registerAnimators(*children)
        animatorSet.start()
    }

    override fun onFinished() {
        cameraPlugin.unregisterAnimators(
            *children,
            cancelAnimators = false,
        )
    }

    override fun cancel() {
        animatorSet.cancel()
        cameraPlugin.unregisterAnimators(
            *children,
            cancelAnimators = false,
        )
    }
}
