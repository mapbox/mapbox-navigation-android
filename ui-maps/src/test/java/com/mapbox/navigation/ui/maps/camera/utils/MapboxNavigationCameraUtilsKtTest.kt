package com.mapbox.navigation.ui.maps.camera.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MercatorCoordinate
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.navigation.ui.maps.internal.camera.constraintDurationTo
import com.mapbox.navigation.ui.maps.internal.camera.normalizeBearing
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.hypot
import kotlin.math.pow

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
    fun `projectedDistance - returns correct distance between two points`() {
        // GIVEN
        val targetZL = 14.0
        val expectedScale = 2.0.pow(targetZL)
        val currentPoint = Point.fromLngLat(0.0, 0.0)
        val targetPoint = Point.fromLngLat(1.0, 1.0)
        val mapboxMap = mockk<MapboxMap> {
            every { project(currentPoint, expectedScale) } returns MercatorCoordinate(10.0, 20.0)
            every { project(targetPoint, expectedScale) } returns MercatorCoordinate(30.0, 50.0)
        }

        // WHEN
        val result = projectedDistance(mapboxMap, currentPoint, targetPoint, targetZL)

        // THEN
        assertEquals(hypot(10.0 - 30.0, 20.0 - 50.0), result, 0.000001)
        verify(exactly = 1) { mapboxMap.project(currentPoint, expectedScale) }
        verify(exactly = 1) { mapboxMap.project(targetPoint, expectedScale) }
    }

    @Test
    fun normalize_projection() {
        val expected = 677.9955562460304

        val actual = normalizeProjection(projectedDistance = 1.23)

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test createAnimatorSet`() {
        // Real animators rather than mockk<Animator>(): from API 34 on, Animator has a
        // package-private nested AnimatorCaller that a ByteBuddy subclass in another
        // classloader cannot access, so mocking Animator itself throws IllegalAccessError.
        val animators = listOf<Animator>(ValueAnimator.ofFloat(), ValueAnimator.ofFloat())
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
