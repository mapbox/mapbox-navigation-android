package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.CarContext
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class CarNavigationCameraTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val carRoutesProvider = mockk<CarRoutesProvider> {
        every { navigationRoutes } returns flowOf(emptyList())
    }

    private fun createCamera() = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = CarCameraMode.OVERVIEW,
        carRoutesProvider = carRoutesProvider,
    )

    @Test
    fun `zoom updates can be disabled before the map surface is attached`() {
        val camera = createCamera()

        camera.zoomUpdatesAllowed(false)

        assertFalse(camera.followingZoomUpdatesAllowed())
    }

    @Test
    fun `zoom updates are allowed by default`() {
        assertTrue(createCamera().followingZoomUpdatesAllowed())
    }

    @Test
    fun `zoom setting is applied to the viewport created on attach`() {
        val camera = createCamera()
        camera.zoomUpdatesAllowed(false)

        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()
        camera.onAttached(carMapSurface)

        val options = camera.viewportDataSource.options
        assertFalse(options.followingFrameOptions.zoomUpdatesAllowed)
        assertFalse(options.overviewFrameOptions.zoomUpdatesAllowed)
        assertFalse(camera.followingZoomUpdatesAllowed())
        camera.onDetached(carMapSurface)
    }

    @Test
    fun `automatic zoom is re-enabled after detach`() {
        // Attaching resets the camera zoom, so a manual zoom level must not stay locked in.
        val camera = createCamera()
        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()
        camera.onAttached(carMapSurface)
        camera.zoomUpdatesAllowed(false)

        camera.onDetached(carMapSurface)
        assertTrue(camera.followingZoomUpdatesAllowed())
        val newCarMapSurface = mockCarMapSurface()
        camera.onAttached(newCarMapSurface)

        assertTrue(camera.viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed)
        assertTrue(camera.viewportDataSource.options.overviewFrameOptions.zoomUpdatesAllowed)
        camera.onDetached(newCarMapSurface)
    }

    private fun mockCarMapSurface(): MapboxCarMapSurface {
        val mapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
        }
        val aMapSurface = mockk<MapSurface> {
            every { getMapboxMap() } returns mapboxMap
            every { camera } returns mockk<CameraAnimationsPlugin>(relaxed = true)
        }
        val aCarContext = mockk<CarContext> {
            every { resources } returns RuntimeEnvironment.getApplication().resources
        }
        return mockk {
            every { mapSurface } returns aMapSurface
            every { carContext } returns aCarContext
        }
    }

    @Test
    fun `map surface is reported as attached only between attach and detach`() {
        val camera = createCamera()
        assertFalse(camera.isMapSurfaceAttached)
        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()

        camera.onAttached(carMapSurface)
        assertTrue(camera.isMapSurfaceAttached)
        camera.onDetached(carMapSurface)

        assertFalse(camera.isMapSurfaceAttached)
    }

    @Test
    fun `zoom flag changes are observable`() {
        val camera = createCamera()

        camera.zoomUpdatesAllowed(false)

        assertFalse(camera.zoomUpdatesAllowedFlow.value)
    }
}
