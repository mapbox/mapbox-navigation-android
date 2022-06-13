package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_FRAME_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_STATE_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.utils.constraintDurationTo
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSet
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSetWith
import com.mapbox.navigation.ui.maps.camera.utils.normalizeBearing
import com.mapbox.navigation.ui.maps.camera.utils.normalizeProjection
import com.mapbox.navigation.ui.maps.camera.utils.projectedDistance
import com.mapbox.navigation.ui.maps.camera.utils.screenDistanceFromMapCenterToTarget
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
        mockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
        every { createAnimatorSet(any()) } returns animatorSet
        every { createAnimatorSetWith(any()) } returns animatorSet
        every { animatorSet.constraintDurationTo(any()) } returns constrainedSet
        every { animatorSet.setDuration(any()) } returns constrainedSet
        every { normalizeProjection(any()) } returns 2000.0
        every { projectedDistance(any(), any(), any(), any()) } returns 1300.0
        every { screenDistanceFromMapCenterToTarget(mapboxMap, any(), any()) } returns 1000.0
        every { cameraPlugin.createCenterAnimator(any(), any()) } returns mockk()
        every { cameraPlugin.createBearingAnimator(any(), any(), any()) } returns mockk()
        every { cameraPlugin.createPitchAnimator(any(), any()) } returns mockk()
        every { cameraPlugin.createZoomAnimator(any(), any()) } returns mockk()
        every { cameraPlugin.createPaddingAnimator(any(), any()) } returns mockk()
        every { mapboxMap.cameraState } returns mockk(relaxed = true)
    }

    @Test
    fun transitionFromLowZoomToHighZoomWhenImmediate() {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(139.7745686, 35.677573))
            .zoom(22.0)
            .build()
        val mockAnimators: Array<CameraAnimator<*>> = arrayOf()
        val cameraPluginImpl = mockk<CameraAnimationsPluginImpl> {
            every { cameraAnimationsFactory } returns mockk {
                every { getFlyTo(cameraOptions) } returns mockAnimators
            }
        }
        every { constrainedSet.duration } returns min(0.0, 1300.0).toLong()
        val transition = MapboxNavigationCameraTransition(mapboxMap, cameraPluginImpl)

        val animator = transition.transitionFromLowZoomToHighZoom(
            cameraOptions,
            NavigationCameraTransitionOptions.Builder().maxDuration(0L).build()
        )

        assertEquals(0L, animator.duration)
        verify { createAnimatorSetWith(any()) }
    }

    @Test
    fun transitionFlyFromLowZoomToHighZoom() {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(139.7745686, 35.677573))
            .build()
        val transition = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)

        transition.transitionFromLowZoomToHighZoom(cameraOptions, DEFAULT_STATE_TRANSITION_OPT)

        verify { createAnimatorSet(any()) }
    }

    @Test
    fun `transitionFromLowZoomToHighZoom - bearing is normalized`() {
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
        transitions.transitionFromLowZoomToHighZoom(cameraOptions, DEFAULT_STATE_TRANSITION_OPT)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
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
    fun `transitionFromLowZoomToHighZoom - duration constrained`() {
        every { mapboxMap.cameraState } returns mockk {
            every { center } returns mockk()
            every { zoom } returns 0.0
            every { bearing } returns 0.0
        }
        val cameraOptions: CameraOptions = mockk(relaxed = true)

        val animator =
            transitions.transitionFromLowZoomToHighZoom(cameraOptions, DEFAULT_STATE_TRANSITION_OPT)

        verify { animatorSet.constraintDurationTo(DEFAULT_STATE_TRANSITION_OPT.maxDuration) }
        assertEquals(constrainedSet, animator)
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

        val animator = transitions.transitionLinear(cameraOptions, DEFAULT_FRAME_TRANSITION_OPT)

        verify { animatorSet.constraintDurationTo(DEFAULT_FRAME_TRANSITION_OPT.maxDuration) }
        assertEquals(constrainedSet, animator)
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
    }
}
