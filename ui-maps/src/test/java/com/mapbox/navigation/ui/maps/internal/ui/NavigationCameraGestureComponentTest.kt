package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class NavigationCameraGestureComponentTest {

    private lateinit var mapView: MapView
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var sut: NavigationCameraGestureComponent

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        navigationCamera = mockk(relaxed = true)
        mapView = mockk(relaxed = true)

        sut = NavigationCameraGestureComponent(mapView, navigationCamera)
    }

    @Test
    fun `should update NavigationCamera to Idle state on map gesture`() {
        val listener = slot<CameraAnimationsLifecycleListener>()
        every { mapView.camera } returns mockk {
            every { addCameraAnimationsLifecycleListener(capture(listener)) } returns Unit
        }
        sut.onAttached(mapboxNavigation)

        listener.captured.onAnimatorStarting(CameraAnimatorType.ANCHOR, mockk(), "owner")

        verify { navigationCamera.requestNavigationCameraToIdle() }
    }
}
