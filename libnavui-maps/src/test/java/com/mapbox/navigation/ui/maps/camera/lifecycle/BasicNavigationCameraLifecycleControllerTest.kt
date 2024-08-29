package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BasicNavigationCameraLifecycleControllerTest {

    private val navigationCamera: NavigationCamera = mockk(relaxUnitFun = true)

    private lateinit var controller: NavigationBasicGesturesHandler

    @Before
    fun setup() {
        controller = NavigationBasicGesturesHandler(navigationCamera)
    }

    @Test
    fun `when any camera transition outside of navigation starts, request idle`() {
        controller.onAnimatorStarting(
            mockk(),
            mockk(),
            "some_owner",
        )
        controller.onAnimatorStarting(
            mockk(),
            mockk(),
            null,
        )

        verify(exactly = 2) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when camera transition starts, do nothing`() {
        controller.onAnimatorStarting(
            mockk(),
            mockk(),
            NAVIGATION_CAMERA_OWNER,
        )

        verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
    }
}
