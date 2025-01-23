package com.mapbox.navigation.ui.maps.camera.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.navigation.ui.maps.camera.internal.constraintDurationTo
import com.mapbox.navigation.ui.maps.camera.internal.normalizeBearing
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxNavigationCameraUtilsKtTest {

    @Test
    fun `test normalizeBearing 1`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = 10.0,
            targetBearing = 0.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 2`() {
        val expected = 10.0

        val actual = normalizeBearing(
            currentBearing = 0.0,
            targetBearing = 10.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 3`() {
        val expected = -0.5

        val actual = normalizeBearing(
            currentBearing = 1.0,
            targetBearing = 359.5,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 4`() {
        val expected = 360.0

        val actual = normalizeBearing(
            currentBearing = 359.5,
            targetBearing = 0.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 5`() {
        val expected = 361.0

        val actual = normalizeBearing(
            currentBearing = 359.5,
            targetBearing = 1.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 6`() {
        val expected = 110.0

        val actual = normalizeBearing(
            currentBearing = 50.0,
            targetBearing = 110.0,
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 7`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = -0.0,
            targetBearing = 360.0,
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 8`() {
        val expected = -0.0

        val actual = normalizeBearing(
            currentBearing = -0.0,
            targetBearing = 0.0,
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 9`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = 27.254667247679752,
            targetBearing = 0.0,
        )

        assertEquals(expected, actual, 1E-14)
    }

    @Test
    fun normalize_projection() {
        val expected = 677.9955562460304

        val actual = normalizeProjection(projectedDistance = 1.23)

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test createAnimatorSet`() {
        val animators = listOf<Animator>(mockk(), mockk())
        val expected = AnimatorSet().apply {
            playTogether(*(animators.toTypedArray()))
        }.childAnimations

        val actual = createAnimatorSet(animators).childAnimations

        assertEquals(expected, actual)
    }

    @Test
    fun `test createAnimatorSetWith`() {
        val animators = arrayOf<CameraAnimator<*>>(mockk(), mockk())
        val expected = AnimatorSet().apply {
            playTogether(*animators)
        }.childAnimations

        val actual = createAnimatorSetWith(animators).childAnimations

        assertEquals(expected, actual)
    }

    @Test
    fun `test constraintDurationTo - no adjustments`() {
        val animators = listOf<Animator>(
            ValueAnimator.ofFloat().apply {
                startDelay = 700
                duration = 1300
            },
            ValueAnimator.ofFloat().apply {
                startDelay = 0
                duration = 1000
            },
        )
        val expected = createAnimatorSet(animators).childAnimations

        val actual = createAnimatorSet(animators)
            .constraintDurationTo(2000)
            .childAnimations

        assertEquals(expected, actual)
    }

    @Test
    fun `test constraintDurationTo - adjustment needed`() {
        val originalAnimators = listOf<Animator>(
            ValueAnimator.ofFloat().apply {
                startDelay = 1000
                duration = 3000
            },
            ValueAnimator.ofFloat().apply {
                startDelay = 0
                duration = 1000
            },
        )
        val expectedAnimators = listOf<Animator>(
            ValueAnimator.ofFloat().apply {
                startDelay = 500
                duration = 1500
            },
            ValueAnimator.ofFloat().apply {
                startDelay = 0
                duration = 500
            },
        )
        val expected = createAnimatorSet(expectedAnimators).childAnimations

        val actual = createAnimatorSet(originalAnimators)
            .constraintDurationTo(2000)
            .childAnimations

        assertEquals(expected[0].startDelay, actual[0].startDelay)
        assertEquals(expected[0].duration, actual[0].duration)
        assertEquals(expected[1].startDelay, actual[1].startDelay)
        assertEquals(expected[1].duration, actual[1].duration)
    }
}
