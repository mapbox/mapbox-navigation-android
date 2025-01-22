package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationCameraFlowableTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun navigationCameraFlowable() = coroutineRule.runBlockingTest {
        val camera = mockk<NavigationCamera>()
        val callbackSlot = slot<NavigationCameraStateChangedObserver>()
        every {
            camera.registerNavigationCameraStateChangeObserver(capture(callbackSlot))
        } just Runs
        every { camera.unregisterNavigationCameraStateChangeObserver(any()) } just Runs
        var actual = NavigationCameraState.FOLLOWING

        val flow = camera.flowNavigationCameraState().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = NavigationCameraState.IDLE
        callbackSlot.captured.onNavigationCameraStateChanged(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { camera.unregisterNavigationCameraStateChangeObserver(callbackSlot.captured) }
    }
}
