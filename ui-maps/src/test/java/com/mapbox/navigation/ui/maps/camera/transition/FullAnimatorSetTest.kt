package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

internal class FullAnimatorSetTest {

    private val cameraPlugin = mockk<CameraAnimationsPlugin>(relaxed = true)
    private val children = arrayListOf<Animator>(mockk<ValueAnimator>(), mockk<ValueAnimator>())
    private val originalAnimatorSet = mockk<AnimatorSet>(relaxed = true) {
        every { childAnimations } returns children
    }

    private val fullAnimatorSet = FullAnimatorSet(cameraPlugin, originalAnimatorSet)

    @Test
    fun addListener() {
        val originalListener = slot<Animator.AnimatorListener>()
        val listener = mockk<MapboxAnimatorSetListener>(relaxed = true)

        clearMocks(originalAnimatorSet)

        fullAnimatorSet.addListener(listener)

        verify { originalAnimatorSet.addListener(capture(originalListener)) }

        verify(exactly = 0) { listener.onAnimationStart(any()) }
        verify(exactly = 0) { listener.onAnimationEnd(any()) }
        verify(exactly = 0) { listener.onAnimationCancel(any()) }

        originalListener.captured.onAnimationStart(mockk())

        verify(exactly = 1) { listener.onAnimationStart(fullAnimatorSet) }
        verify(exactly = 0) { listener.onAnimationEnd(any()) }
        verify(exactly = 0) { listener.onAnimationCancel(any()) }

        clearMocks(listener)

        originalListener.captured.onAnimationEnd(mockk())

        verify(exactly = 0) { listener.onAnimationStart(any()) }
        verify(exactly = 1) { listener.onAnimationEnd(fullAnimatorSet) }
        verify(exactly = 0) { listener.onAnimationCancel(any()) }

        clearMocks(listener)

        originalListener.captured.onAnimationCancel(mockk())

        verify(exactly = 0) { listener.onAnimationStart(any()) }
        verify(exactly = 0) { listener.onAnimationEnd(any()) }
        verify(exactly = 1) { listener.onAnimationCancel(fullAnimatorSet) }

        clearMocks(listener)

        originalListener.captured.onAnimationRepeat(mockk())

        verify(exactly = 0) { listener.onAnimationStart(any()) }
        verify(exactly = 0) { listener.onAnimationEnd(any()) }
        verify(exactly = 0) { listener.onAnimationCancel(any()) }
    }

    @Test
    fun addEndListener() {
        val originalListener = slot<Animator.AnimatorListener>()
        val listener = mockk<MapboxAnimatorSetEndListener>(relaxed = true)

        fullAnimatorSet.addAnimationEndListener(listener)

        verify { originalAnimatorSet.addListener(capture(originalListener)) }

        verify(exactly = 0) { listener.onAnimationEnd(any()) }

        originalListener.captured.onAnimationStart(mockk())
        originalListener.captured.onAnimationCancel(mockk())
        originalListener.captured.onAnimationRepeat(mockk())

        verify(exactly = 0) { listener.onAnimationEnd(any()) }

        originalListener.captured.onAnimationEnd(mockk())

        verifyOrder {
            cameraPlugin.unregisterAnimators(
                *children.map { it as ValueAnimator }.toTypedArray(),
                cancelAnimators = false,
            )
            listener.onAnimationEnd(fullAnimatorSet)
        }
        originalListener.captured.onAnimationEnd(mockk())

        verify(exactly = 2) { listener.onAnimationEnd(fullAnimatorSet) }
    }

    @Test
    fun makeInstant() {
        fullAnimatorSet.makeInstant()

        verify { originalAnimatorSet.setDuration(0) }
    }

    @Test
    fun start() {
        fullAnimatorSet.start()

        verifyOrder {
            cameraPlugin.registerAnimators(*children.map { it as ValueAnimator }.toTypedArray())
            originalAnimatorSet.start()
        }
    }

    @Test
    fun onFinishedWithoutExternalListeners() {
        val originalListenerSlot = slot<AnimatorListener>()
        verify { originalAnimatorSet.addListener(capture(originalListenerSlot)) }
        originalListenerSlot.captured.onAnimationEnd(originalAnimatorSet)

        verify {
            cameraPlugin.unregisterAnimators(
                *children.map { it as ValueAnimator }.toTypedArray(),
                cancelAnimators = false,
            )
        }
    }

    @Test
    fun cancel() {
        fullAnimatorSet.cancel()

        verifyOrder {
            originalAnimatorSet.cancel()
            cameraPlugin.unregisterAnimators(
                *children.map { it as ValueAnimator }.toTypedArray(),
                cancelAnimators = false,
            )
        }
    }
}
