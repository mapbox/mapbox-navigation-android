package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import com.mapbox.maps.CameraAnimationHint
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.maps.plugin.animation.calculateCameraAnimationHint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class SimplifiedAnimatorSetTest {

    private val cameraPlugin = mockk<CameraAnimationsPlugin>(relaxed = true)
    private val child1 = mockk<ValueAnimator>(relaxed = true)
    private val child2 = mockk<ValueAnimator>(relaxed = true)
    private val children = arrayListOf(child1, child2)
    private val mapboxMap = mockk<MapboxMap>(relaxed = true)
    private val animatorSet = SimplifiedAnimatorSet(cameraPlugin, mapboxMap, children)

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.maps.plugin.animation.CameraAnimationsUtils")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.maps.plugin.animation.CameraAnimationsUtils")
    }

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

        verifyOrder {
            cameraPlugin.unregisterAnimators(*children.toTypedArray(), cancelAnimators = false)
            listener.onAnimationEnd(animatorSet)
        }
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
    fun startWithCameraAnimators() {
        val fractions = listOf(0.25f, 0.5f, 0.75f, 1f)
        val cameraAnimator1 = mockk<CameraAnimator<*>>(relaxed = true)
        val cameraAnimator2 = mockk<CameraAnimator<*>>(relaxed = true)
        val cameraAnimators = listOf(cameraAnimator1, cameraAnimator2)
        val cameraChildren = arrayListOf<ValueAnimator>(cameraAnimator1, cameraAnimator2)
        val cameraAnimatorSet = SimplifiedAnimatorSet(cameraPlugin, mapboxMap, cameraChildren)

        val cameraState = mockk<CameraState>()
        every { mapboxMap.cameraState } returns cameraState

        val mockHint = mockk<CameraAnimationHint>()
        every {
            cameraAnimators.calculateCameraAnimationHint(
                fractions,
                cameraState,
            )
        } returns mockHint

        cameraAnimatorSet.start()

        verifyOrder {
            cameraPlugin.registerAnimators(*cameraChildren.toTypedArray())
            cameraAnimators.calculateCameraAnimationHint(fractions, cameraState)
            mapboxMap.setCameraAnimationHint(mockHint)
            cameraAnimator1.start()
            cameraAnimator2.start()
        }
    }

    @Test
    fun startWithMixedAnimators() {
        val cameraAnimator = mockk<CameraAnimator<*>>(relaxed = true)
        val valueAnimator = mockk<ValueAnimator>(relaxed = true)
        val mixedChildren = arrayListOf<ValueAnimator>(cameraAnimator, valueAnimator)
        val mixedAnimatorSet = SimplifiedAnimatorSet(cameraPlugin, mapboxMap, mixedChildren)

        mixedAnimatorSet.start()

        verifyOrder {
            cameraPlugin.registerAnimators(*mixedChildren.toTypedArray())
            cameraAnimator.start()
            valueAnimator.start()
        }

        // Should not call setCameraAnimationHint when not all children are CameraAnimators
        verify(exactly = 0) { mapboxMap.setCameraAnimationHint(any()) }
    }

    @Test
    fun onFinishedWithoutExternalListeners() {
        val originalListenerSlot = slot<AnimatorListener>()
        verify { child1.addListener(capture(originalListenerSlot)) }
        originalListenerSlot.captured.onAnimationEnd(child1)
        originalListenerSlot.captured.onAnimationEnd(child2)

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
