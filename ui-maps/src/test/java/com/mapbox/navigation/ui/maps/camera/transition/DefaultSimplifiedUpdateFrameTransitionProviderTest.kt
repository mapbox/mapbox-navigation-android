package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.ValueAnimator
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_FRAME_TRANSITION_OPT
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

class DefaultSimplifiedUpdateFrameTransitionProviderTest {

    private val mapboxMap: MapboxMap = mockk(relaxed = true)
    private val cameraPlugin: CameraAnimationsPlugin = mockk(relaxed = true)
    private val updateFrame = DefaultSimplifiedUpdateFrameTransitionProvider(
        mapboxMap,
        cameraPlugin,
    )

    @Before
    fun setUp() {
        mockkStatic(::normalizeBearing)
    }

    @After
    fun tearDown() {
        unmockkStatic(::normalizeBearing)
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
        updateFrame.updateFrame(cameraOptions, DEFAULT_FRAME_TRANSITION_OPT)

        assertEquals(-10.0, valueSlot.captured.targets.last(), 0.0000000001)
        verify { normalizeBearing(10.0, 350.0) }
    }

    @Test
    fun `transitionLinear - duration constrained`() {
        every { mapboxMap.cameraState } returns mockk {
            every { center } returns mockk()
            every { zoom } returns 0.0
            every { bearing } returns 0.0
        }
        val cameraOptions: CameraOptions = mockk(relaxed = true)

        val centerAnimator = mockk<ValueAnimator>(relaxed = true)
        val bearingAnimator = mockk<ValueAnimator>(relaxed = true)
        val pitchAnimator = mockk<ValueAnimator>(relaxed = true)
        val zoomAnimator = mockk<ValueAnimator>(relaxed = true)
        val paddingAnimator = mockk<ValueAnimator>(relaxed = true)
        every {
            cameraPlugin.createCenterAnimator(any(), any<(ValueAnimator.() -> Unit)>())
        } returns centerAnimator
        every { cameraPlugin.createBearingAnimator(any(), any(), any()) } returns bearingAnimator
        every { cameraPlugin.createPitchAnimator(any(), any()) } returns pitchAnimator
        every { cameraPlugin.createZoomAnimator(any(), any()) } returns zoomAnimator
        every { cameraPlugin.createPaddingAnimator(any(), any()) } returns paddingAnimator

        val maxDuration = 700L

        val animations = updateFrame.updateFrame(
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

        assertEquals(
            listOf(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, paddingAnimator),
            animations,
        )
    }

    private fun verifyOptionsBlock(block: ValueAnimator.() -> Unit, maxDuration: Long) {
        val original = mockk<ValueAnimator>(relaxed = true)
        original.block()
        verify { original.duration = maxDuration }
    }
}
