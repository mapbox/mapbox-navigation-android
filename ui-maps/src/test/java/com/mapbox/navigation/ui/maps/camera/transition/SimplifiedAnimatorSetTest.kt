package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

internal class SimplifiedAnimatorSetTest {

    private val cameraPlugin = mockk<CameraAnimationsPlugin>(relaxed = true)
    private val child1 = mockk<ValueAnimator>(relaxed = true)
    private val child2 = mockk<ValueAnimator>(relaxed = true)
    private val children = arrayListOf(child1, child2)
    private val animatorSet = SimplifiedAnimatorSet(cameraPlugin, children)

    @Test
    fun addAnimationEndListener() {
        val listener = mockk<MapboxAnimatorSetEndListener>(relaxed = true)
        val originalListenerSlot = slot<AnimatorListener>()

        animatorSet.addAnimationEndListener(listener)

        verify { child1.addListener(capture(originalListenerSlot)) }
        val originalListener = originalListenerSlot.captured
        verify { child2.addListener(originalListener) }

        verify(exactly = 0) { listener.onAnimationEnd(any()) }

        originalListener.onAnimationStart(child1)
        originalListener.onAnimationStart(child2)
        originalListener.onAnimationCancel(child1)
        originalListener.onAnimationCancel(child2)

        verify(exactly = 0) { listener.onAnimationEnd(any()) }

        originalListener.onAnimationEnd(child2)

        verify(exactly = 0) { listener.onAnimationEnd(any()) }

        originalListener.onAnimationEnd(child1)

        verify(exactly = 1) { listener.onAnimationEnd(animatorSet) }
    }

    @Test
    fun makeInstant() {
        animatorSet.makeInstant()

        verify { child1.duration = 0 }
        verify { child2.duration = 0 }
    }

    @Test
    fun start() {
        animatorSet.start()

        verifyOrder {
            cameraPlugin.registerAnimators(*children.toTypedArray())
            child1.start()
            child2.start()
        }
    }

    @Test
    fun onFinished() {
        animatorSet.onFinished()

        verify {
            cameraPlugin.unregisterAnimators(*children.toTypedArray(), cancelAnimators = false)
        }
    }

    @Test
    fun cancel() {
        animatorSet.cancel()

        verify {
            cameraPlugin.unregisterAnimators(*children.toTypedArray(), cancelAnimators = true)
        }
    }
}
