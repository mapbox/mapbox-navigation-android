package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class MapboxCameraModeButtonComponentContractTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var navigationCamera: NavigationCamera
    private lateinit var sut: MapboxCameraModeButtonComponentContract

    @Before
    fun setUp() {
        navigationCamera = mockk(relaxed = true)
        sut = MapboxCameraModeButtonComponentContract {
            navigationCamera
        }
    }

    @Test
    fun `onAttached should observe navigationCamera state and update buttonState`() =
        runBlockingTest {
            val observer = slot<NavigationCameraStateChangedObserver>()
            every {
                navigationCamera.registerNavigationCameraStateChangeObserver(capture(observer))
            } returns Unit

            sut.onAttached(mockk())

            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.OVERVIEW)
            assertEquals(NavigationCameraState.OVERVIEW, sut.buttonState.value)

            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)
            assertEquals(NavigationCameraState.FOLLOWING, sut.buttonState.value)
        }

    @Test
    fun `onAttached should not update buttonState when NavigationCameraState is IDLE`() =
        runBlockingTest {
            val observer = captureNavigationCameraStateChangedObserver(navigationCamera)

            sut.onAttached(mockk())

            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)
            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.IDLE)
            assertEquals(NavigationCameraState.FOLLOWING, sut.buttonState.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `onClick should update navigationCamera to following when NavigationCameraState is OVERVIEW`() =
        runBlockingTest {
            val observer = captureNavigationCameraStateChangedObserver(navigationCamera)
            sut.onAttached(mockk())
            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.OVERVIEW)

            sut.onClick(mockk())

            verify { navigationCamera.requestNavigationCameraToFollowing() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `onClick should update navigationCamera to overview when NavigationCameraState is FOLLOWING`() =
        runBlockingTest {
            val observer = captureNavigationCameraStateChangedObserver(navigationCamera)
            sut.onAttached(mockk())
            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)

            sut.onClick(mockk())

            verify { navigationCamera.requestNavigationCameraToOverview() }
        }

    private fun captureNavigationCameraStateChangedObserver(
        navigationCamera: NavigationCamera
    ): CapturingSlot<NavigationCameraStateChangedObserver> {
        val observer = slot<NavigationCameraStateChangedObserver>()
        every {
            navigationCamera.registerNavigationCameraStateChangeObserver(capture(observer))
        } returns Unit
        return observer
    }
}
