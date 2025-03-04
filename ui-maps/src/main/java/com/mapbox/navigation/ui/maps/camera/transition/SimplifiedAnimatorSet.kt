package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator

/**
 * Only supports simple animators dependencies:
 * 1. Playing simultaneously;
 * 2. No start delays.
 *
 * Used internal to avoid using [AnimatorSet.start] to make starting animations more performant.
 */
internal class SimplifiedAnimatorSet(
    override val children: List<Animator>,
) : MapboxAnimatorSet {

    override fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener) {
        val simplifiedAnimatorSetListener = SimplifiedAnimatorSetEndListener(listener)
        children.forEach { it.addListener(simplifiedAnimatorSetListener) }
    }

    override fun makeInstant() {
        children.forEach { it.duration = 0 }
    }

    override fun start() {
        children.forEach { it.start() }
    }

    override fun end() {
        children.forEach { it.end() }
    }

    override fun cancel() {
        children.forEach { it.cancel() }
    }

    private inner class SimplifiedAnimatorSetEndListener(
        private val originalListener: MapboxAnimatorSetEndListener,
    ) : Animator.AnimatorListener {

        private val nonFinishedChildren = children.toMutableList()

        override fun onAnimationStart(animation: Animator) {
            // no-op
        }

        override fun onAnimationEnd(animation: Animator) {
            nonFinishedChildren.remove(animation)
            if (nonFinishedChildren.isEmpty()) {
                originalListener.onAnimationEnd(this@SimplifiedAnimatorSet)
            }
        }

        override fun onAnimationCancel(animation: Animator) {
            // no-op
        }

        override fun onAnimationRepeat(animation: Animator) {
            // no-op
        }
    }
}
