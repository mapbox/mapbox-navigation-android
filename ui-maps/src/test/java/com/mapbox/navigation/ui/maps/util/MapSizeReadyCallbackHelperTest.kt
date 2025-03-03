package com.mapbox.navigation.ui.maps.util

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class MapSizeReadyCallbackHelperTest {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var helper: MapSizeReadyCallbackHelper

    @Before
    fun setup() {
        mapboxMap = mockk(relaxed = true)
        helper = MapSizeReadyCallbackHelper(mapboxMap)
    }

    private fun mockCameraForCoordinates(): CapturingSlot<(CameraOptions) -> Unit> {
        val callbackSlot = slot<(CameraOptions) -> Unit>()
        every {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(callbackSlot),
            )
        } returns Unit
        return callbackSlot
    }

    @Test
    fun `notifies when map size is ready`() {
        val cameraForCoordinatesCallbackSlot = mockCameraForCoordinates()

        val action = mockk<() -> Unit>(relaxed = true)
        helper.onMapSizeReady(action)

        cameraForCoordinatesCallbackSlot.captured.invoke(mockk())

        verify(exactly = 1) {
            action.invoke()
        }
    }

    @Test
    fun `notifies only once when map size is ready`() {
        val cameraForCoordinatesCallbackSlot = mockCameraForCoordinates()

        val action = mockk<() -> Unit>(relaxed = true)
        helper.onMapSizeReady(action)

        cameraForCoordinatesCallbackSlot.captured.invoke(mockk())
        cameraForCoordinatesCallbackSlot.captured.invoke(mockk())

        verify(exactly = 1) {
            action.invoke()
        }
    }

    @Test
    fun `does not notify if request is cancelled`() {
        val cameraForCoordinatesCallbackSlot = mockCameraForCoordinates()

        val action = mockk<() -> Unit>(relaxed = true)
        val request = helper.onMapSizeReady(action)
        request.cancel()

        cameraForCoordinatesCallbackSlot.captured.invoke(mockk())

        verify(exactly = 0) {
            action.invoke()
        }
    }
}
