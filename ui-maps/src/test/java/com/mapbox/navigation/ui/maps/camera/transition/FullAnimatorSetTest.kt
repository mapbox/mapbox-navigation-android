package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FullAnimatorSetTest {

    private val children = arrayListOf<Animator>(mockk<ValueAnimator>(), mockk<ValueAnimator>())
    private val originalAnimatorSet = mockk<AnimatorSet>(relaxed = true) {
        every { childAnimations } returns children
    }

    private val fullAnimatorSet = FullAnimatorSet(originalAnimatorSet)

    @Test
    fun children() {
        assertEquals(children, fullAnimatorSet.children)
    }

    @Test
    fun addListener() {
        val originalListener = slot<Animator.AnimatorListener>()
        val listener = mockk<MapboxAnimatorSetListener>(relaxed = true)

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

        verify(exactly = 1) { listener.onAnimationEnd(fullAnimatorSet) }
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

        verify { originalAnimatorSet.start() }
    }

    @Test
    fun end() {
        fullAnimatorSet.end()

        verify { originalAnimatorSet.end() }
    }

    @Test
    fun cancel() {
        fullAnimatorSet.cancel()

        verify { originalAnimatorSet.cancel() }
    }
}
