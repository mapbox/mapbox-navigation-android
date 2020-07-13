package com.mapbox.navigation.ui.internal.utils

import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class RouteLineValueAnimatorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun start() = coroutineRule.runBlockingTest {
        var callbackValue: Float = 10f
        val animator = RouteLineValueAnimator().also {
            it.valueAnimatorHandler = { value ->
                callbackValue = value
            }
        }

        animator.start(0f, 5f)
        advanceTimeBy(5000)

        assertEquals(5.0f, callbackValue)
    }

    @Test
    fun startThirtyFramesPerSecond() = coroutineRule.runBlockingTest {
        var callbackValue: Float = 10f
        val animator = RouteLineValueAnimator().also {
            it.valueAnimatorHandler = { value ->
                callbackValue = value
            }
        }

        animator.start(0f, 5f)
        advanceTimeBy(AnimatorFrequency.ThirtyFramesPerSecond.frequency * 2)
        animator.cancelAnimationCallbacks()

        assertEquals(0.25f, callbackValue)
    }

    @Test
    fun startSixtyFramesPerSecond() = coroutineRule.runBlockingTest {
        var callbackValue: Float = 10f
        val animator = RouteLineValueAnimator(AnimatorFrequency.SixtyFramesPerSecond).also {
            it.valueAnimatorHandler = { value ->
                callbackValue = value
            }
        }

        animator.start(0f, 4f)
        advanceTimeBy(AnimatorFrequency.SixtyFramesPerSecond.frequency * 3)
        animator.cancelAnimationCallbacks()

        assertEquals(0.26666668f, callbackValue)
    }

    @Test
    fun startWhenCancelCalled() = coroutineRule.runBlockingTest {
        var callbackValue: Float = 10f
        val animator = RouteLineValueAnimator().also {
            it.valueAnimatorHandler = { value ->
                callbackValue = value
            }
        }

        animator.start(0f, 5f)
        animator.cancelAnimationCallbacks()
        advanceTimeBy(5000)

        assertEquals(0.083333336f, callbackValue)
    }

    @Test
    fun startReset() = coroutineRule.runBlockingTest {
        var callbackValue: Float = 10f
        val animator = RouteLineValueAnimator().also {
            it.valueAnimatorHandler = { value ->
                callbackValue = value
            }
        }
        animator.start(0f, 5f)
        ThreadController.cancelAllUICoroutines()
        animator.reInitialize()

        animator.start(0f, 4f)
        advanceTimeBy(5000)

        assertEquals(3.9999988f, callbackValue)
    }
}
