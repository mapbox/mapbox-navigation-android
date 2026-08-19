package com.mapbox.navigation.ui.maps.util

import com.mapbox.common.Cancelable
import com.mapbox.maps.MapboxMap
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class MapSizeInitializedCallbackHelperTest {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var helper: MapSizeInitializedCallbackHelper

    @Before
    fun setup() {
        mapboxMap = mockk(relaxed = true)
        helper = MapSizeInitializedCallbackHelper(mapboxMap)
    }

    private fun mockWhenSizeReady(): CapturingSlot<() -> Unit> {
        val callbackSlot = slot<() -> Unit>()
        every {
            mapboxMap.whenSizeReady(
                capture(callbackSlot),
            )
        } returns mockk(relaxed = true)
        return callbackSlot
    }

    @Test
    fun `notifies when map size is ready`() {
        val actionSlot = mockWhenSizeReady()

        val action = mockk<() -> Unit>(relaxed = true)
        helper.onMapSizeInitialized(action)

        actionSlot.captured.invoke()

        verify(exactly = 1) {
            action.invoke()
        }
    }

    @Test
    fun `notifies only once when map size is ready`() {
        val actionSlot = mockWhenSizeReady()

        val action = mockk<() -> Unit>(relaxed = true)
        helper.onMapSizeInitialized(action)

        actionSlot.captured.invoke()
        actionSlot.captured.invoke()

        verify(exactly = 1) {
            action.invoke()
        }
    }

    @Test
    fun `does not notify if request is cancelled`() {
        val actionSlot = mockWhenSizeReady()

        val action = mockk<() -> Unit>(relaxed = true)
        val request = helper.onMapSizeInitialized(action)
        request.cancel()

        actionSlot.captured.invoke()

        verify(exactly = 0) {
            action.invoke()
        }
    }

    @Test
    fun `cancel removes the registration from the map's size-ready registry`() {
        val actionSlot = slot<() -> Unit>()
        val registration = mockk<Cancelable>(relaxed = true)
        every {
            mapboxMap.whenSizeReady(
                capture(actionSlot),
            )
        } returns registration

        val action = mockk<() -> Unit>(relaxed = true)
        val request = helper.onMapSizeInitialized(action)
        request.cancel()

        verify(exactly = 1) {
            registration.cancel()
        }
    }

    @Test
    fun `notifies only once when action throws`() {
        val actionSlot = mockWhenSizeReady()

        val action = mockk<() -> Unit>()
        every { action.invoke() } throws RuntimeException()
        helper.onMapSizeInitialized(action)

        assertThrows(RuntimeException::class.java) {
            actionSlot.captured.invoke()
        }
        actionSlot.captured.invoke()

        verify(exactly = 1) {
            action.invoke()
        }
    }
}
