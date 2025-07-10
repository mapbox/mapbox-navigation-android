package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.ValueAnimator
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.maps.plugin.animation.calculateCameraAnimationHint
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Only supports simple animators dependencies:
 * 1. Playing simultaneously;
 * 2. No start delays.
 *
 * Used internal to avoid using [AnimatorSet.start] to make starting animations more performant.
 */
internal class SimplifiedAnimatorSet(
    private val cameraPlugin: CameraAnimationsPlugin,
    private val mapboxMap: MapboxMap,
    children: List<ValueAnimator>,
) : MapboxAnimatorSet {

    // Calculate camera animation hints for the specified fractions to pre-download tiles.
    // These values are totally based on heuristics and can be changed in the future.
    private val fractions = listOf(0.25f, 0.5f, 0.75f, 1f)

    private val children = children.toTypedArray()
    private val externalEndListeners = CopyOnWriteArrayList<MapboxAnimatorSetEndListener>()

    init {
        val simplifiedAnimatorSetListener = SimplifiedAnimatorSetEndListener(
            object : MapboxAnimatorSetEndListener {
                override fun onAnimationEnd(animation: MapboxAnimatorSet) {
                    onFinished()
                    externalEndListeners.forEach { it.onAnimationEnd(animation) }
                }
            },
        )
        this.children.forEach { it.addListener(simplifiedAnimatorSetListener) }
    }

    override fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener) {
        externalEndListeners.add(listener)
    }

    override fun makeInstant() {
        children.forEach { it.duration = 0 }
    }

    @OptIn(MapboxExperimental::class)
    override fun start() {
        cameraPlugin.registerAnimators(*children)
        val cameraChildren = children.filterIsInstance<CameraAnimator<*>>()
        if (cameraChildren.size == children.size) {
            cameraChildren.calculateCameraAnimationHint(fractions, mapboxMap.cameraState)?.let {
                mapboxMap.setCameraAnimationHint(it)
            }
        }
        children.forEach { it.start() }
    }

    private fun onFinished() {
        cameraPlugin.unregisterAnimators(
            *children,
            cancelAnimators = false,
        )
    }

    override fun cancel() {
        cameraPlugin.unregisterAnimators(
            *children,
            cancelAnimators = true,
        )
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
