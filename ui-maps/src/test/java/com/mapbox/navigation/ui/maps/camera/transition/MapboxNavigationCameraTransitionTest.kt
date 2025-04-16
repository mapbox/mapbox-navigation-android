package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_FRAME_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_STATE_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSet
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSetWith
import com.mapbox.navigation.ui.maps.camera.utils.getAnimatorsFactory
import com.mapbox.navigation.ui.maps.camera.utils.normalizeProjection
import com.mapbox.navigation.ui.maps.camera.utils.projectedDistance
import com.mapbox.navigation.ui.maps.camera.utils.screenDistanceFromMapCenterToTarget
import com.mapbox.navigation.ui.maps.internal.camera.constraintDurationTo
import com.mapbox.navigation.ui.maps.internal.camera.normalizeBearing
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.min

class MapboxNavigationCameraTransitionTest {

    private val mapboxMap: MapboxMap = mockk()
    private val cameraPlugin: CameraAnimationsPlugin = mockk()
    private val transitions = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)
    private val animatorSet: AnimatorSet = mockk()
    private val constrainedSet: AnimatorSet = mockk()

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.ui.maps.internal.camera.MapboxNavigationCameraUtilsKt")
        mockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
        every { createAnimatorSet(any()) } returns animatorSet
        every { createAnimatorSetWith(any()) } returns animatorSet
        every { animatorSet.constraintDurationTo(any()) } returns constrainedSet
        every { animatorSet.setDuration(any()) } returns constrainedSet
        every { normalizeProjection(any()) } returns 2000.0
        every { projectedDistance(any(), any(), any(), any()) } returns 1300.0
        every { screenDistanceFromMapCenterToTarget(mapboxMap, any(), any()) } returns 1000.0
        every {
            cameraPlugin.createCenterAnimator(any(), any<(ValueAnimator.() -> Unit)>())
        } returns mockk()
        every { cameraPlugin.createBearingAnimator(any(), any(), any()) } returns mockk()
        every { cameraPlugin.createPitchAnimator(any(), any()) } returns mockk()
        every { cameraPlugin.createZoomAnimator(any(), any()) } returns mockk()
        every { cameraPlugin.createPaddingAnimator(any(), any()) } returns mockk()
        every { mapboxMap.cameraState } returns mockk(relaxed = true)
        every { cameraPlugin.getAnimatorsFactory() } returns mockk(relaxed = true)
    }

    @Test
    fun transitionFromLowZoomToHighZoomWhenImmediate() {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(139.7745686, 35.677573))
            .zoom(22.0)
            .build()
        val mockAnimators: Array<CameraAnimator<*>> = arrayOf()
        every { cameraPlugin.getAnimatorsFactory() } returns mockk {
            every { getFlyTo(cameraOptions, NAVIGATION_CAMERA_OWNER) } returns mockAnimators
        }
        every { constrainedSet.duration } returns min(0.0, 1300.0).toLong()
        val transition = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)

        val animator = transition.transitionFromLowZoomToHighZoom(
            cameraOptions,
            NavigationCameraTransitionOptions.Builder().maxDuration(0L).build(),
        )

        assertEquals(0L, animator.duration)
        verify { createAnimatorSetWith(any()) }
    }

    @Test
    fun `transitionFromHighZoomToLowZoom - bearing is normalized`() {
        every { mapboxMap.cameraState } returns mockk {
            every { bearing } returns 10.0
        }

        val cameraOptions = CameraOptions.Builder()
            .bearing(350.0)
            .build()

        val valueSlot = slot<CameraAnimatorOptions<Double>>()
        every {
            cameraPlugin.createBearingAnimator(capture(valueSlot), any(), any())
        } returns mockk()
        transitions.transitionFromHighZoomToLowZoom(cameraOptions, DEFAULT_STATE_TRANSITION_OPT)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @Test
    fun `transitionLinear - bearing is normalized`() {
        every { mapboxMap.cameraState } returns mockk {
            every { bearing } returns 10.0
        }
        val cameraOptions = CameraOptions.Builder()
            .bearing(350.0)
            .build()

        val valueSlot = slot<CameraAnimatorOptions<Double>>()
        every {
            cameraPlugin.createBearingAnimator(capture(valueSlot), any(), any())
        } returns mockk()
        transitions.transitionLinear(cameraOptions, DEFAULT_FRAME_TRANSITION_OPT)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @Test
    fun `transitionFromHighZoomToLowZoom - duration constrained`() {
        every { mapboxMap.cameraState } returns mockk {
            every { center } returns mockk()
            every { zoom } returns 0.0
            every { bearing } returns 0.0
        }
        val cameraOptions: CameraOptions = mockk(relaxed = true)

        val animator =
            transitions.transitionFromHighZoomToLowZoom(cameraOptions, DEFAULT_STATE_TRANSITION_OPT)

        verify { animatorSet.constraintDurationTo(DEFAULT_STATE_TRANSITION_OPT.maxDuration) }
        assertEquals(constrainedSet, animator)
    }

    @Test
    fun `transitionLinear - duration constrained`() {
        every { mapboxMap.cameraState } returns mockk {
            every { center } returns mockk()
            every { zoom } returns 0.0
            every { bearing } returns 0.0
        }
        val cameraOptions: CameraOptions = mockk(relaxed = true)

        val maxDuration = 700L

        val animator = transitions.transitionLinear(
            cameraOptions,
            NavigationCameraTransitionOptions.Builder().maxDuration(maxDuration).build(),
        )

        val centerBlockSlot = slot<(ValueAnimator.() -> Unit)>()
        verify {
            cameraPlugin.createCenterAnimator(
                any(),
                capture(centerBlockSlot),
            )
        }
        verifyOptionsBlock(centerBlockSlot.captured, maxDuration)

        val bearingBlockSlot = slot<(ValueAnimator.() -> Unit)>()
        verify {
            cameraPlugin.createBearingAnimator(
                any(),
                any(),
                capture(bearingBlockSlot),
            )
        }
        verifyOptionsBlock(bearingBlockSlot.captured, maxDuration)

        val paddingBlockSlot = slot<(ValueAnimator.() -> Unit)>()
        verify {
            cameraPlugin.createPaddingAnimator(
                any(),
                capture(paddingBlockSlot),
            )
        }
        verifyOptionsBlock(paddingBlockSlot.captured, maxDuration)

        val pitchBlockSlot = slot<(ValueAnimator.() -> Unit)>()
        verify {
            cameraPlugin.createPitchAnimator(
                any(),
                capture(pitchBlockSlot),
            )
        }
        verifyOptionsBlock(paddingBlockSlot.captured, maxDuration)

        val zoomBlockSlot = slot<(ValueAnimator.() -> Unit)>()
        verify {
            cameraPlugin.createZoomAnimator(
                any(),
                capture(zoomBlockSlot),
            )
        }
        verifyOptionsBlock(zoomBlockSlot.captured, maxDuration)

        assertEquals(animatorSet, animator)
    }

    @Test
    fun simplifiedUpdateFrameTransitionIsUsedForLinear() {
        val updateFrame = mockk<SimplifiedUpdateFrameTransition>(relaxed = true)
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(139.7745686, 35.677573))
            .zoom(22.0)
            .build()
        val transitionOptions = NavigationCameraTransitionOptions.Builder().maxDuration(700).build()
        val child1 = mockk<ValueAnimator>()
        val child2 = mockk<ValueAnimator>()
        every {
            updateFrame.updateFrame(cameraOptions, transitionOptions)
        } returns listOf(child1, child2)
        val transition = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin, updateFrame)

        val actual = transition.transitionLinear(cameraOptions, transitionOptions)

        assertEquals(animatorSet, actual)
        verify { createAnimatorSet(listOf(child1, child2)) }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.internal.camera.MapboxNavigationCameraUtilsKt")
        unmockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
    }

    private fun verifyOptionsBlock(block: ValueAnimator.() -> Unit, maxDuration: Long) {
        val original = mockk<ValueAnimator>(relaxed = true)
        original.block()
        verify { original.duration = maxDuration }
    }
}
