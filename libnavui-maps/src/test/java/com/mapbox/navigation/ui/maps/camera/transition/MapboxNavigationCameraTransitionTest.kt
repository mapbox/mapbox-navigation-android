package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.utils.normalizeBearing
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

class MapboxNavigationCameraTransitionTest {

    private val mapboxMap: MapboxMap = mockk()
    private val cameraPlugin: CameraAnimationsPlugin = mockk()
    private val transitions = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
    }

    @Test
    fun `transitionFromLowZoomToHighZoom - bearing is normalized`() {
        every { mapboxMap.getCameraOptions() } returns CameraOptions.Builder()
            .bearing(10.0)
            .build()
        val cameraOptions = CameraOptions.Builder()
            .bearing(350.0)
            .build()

        val valueSlot = slot<CameraAnimatorOptions<Double>>()
        every { cameraPlugin.createBearingAnimator(capture(valueSlot), any()) } returns mockk()
        transitions.transitionFromLowZoomToHighZoom(cameraOptions)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @Test
    fun `transitionFromHighZoomToLowZoom - bearing is normalized`() {
        every { mapboxMap.getCameraOptions() } returns CameraOptions.Builder()
            .bearing(10.0)
            .build()
        val cameraOptions = CameraOptions.Builder()
            .bearing(350.0)
            .build()

        val valueSlot = slot<CameraAnimatorOptions<Double>>()
        every { cameraPlugin.createBearingAnimator(capture(valueSlot), any()) } returns mockk()
        transitions.transitionFromHighZoomToLowZoom(cameraOptions)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @Test
    fun `transitionLinear - bearing is normalized`() {
        every { mapboxMap.getCameraOptions() } returns CameraOptions.Builder()
            .bearing(10.0)
            .build()
        val cameraOptions = CameraOptions.Builder()
            .bearing(350.0)
            .build()

        val valueSlot = slot<CameraAnimatorOptions<Double>>()
        every { cameraPlugin.createBearingAnimator(capture(valueSlot), any()) } returns mockk()
        transitions.transitionLinear(cameraOptions)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
    }
}
