package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CarLocationsOverviewCameraTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @Test
    fun loaded() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
        val mapboxMap = mockk<MapboxMap>(relaxUnitFun = true)
        val cameraAnimationsPlugin = mockk<CameraAnimationsPlugin>()
        val aMapSurface = mockk<MapSurface> {
            every { getMapboxMap() } returns mapboxMap
            every { camera } returns cameraAnimationsPlugin
        }
        val mapboxCarMapSurface = mockk<MapboxCarMapSurface> {
            every { mapSurface } returns aMapSurface
        }
        val camera = CarLocationsOverviewCamera()

        carAppTestRule.onAttached(mapboxNavigation)
        camera.onAttached(mapboxCarMapSurface)

        verify { mapboxMap.setCamera(any<CameraOptions>()) }
        verify { mapboxNavigation.registerLocationObserver(any()) }
        assertNotNull(camera.viewportDataSource)
        assertNotNull(camera.navigationCamera)
    }

    @Test
    fun detached() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
        val aMapSurface = mockk<MapSurface>()
        val mapboxCarMapSurface = mockk<MapboxCarMapSurface> {
            every { mapSurface } returns aMapSurface
        }
        val camera = CarLocationsOverviewCamera()

        carAppTestRule.onAttached(mapboxNavigation)
        camera.onDetached(mapboxCarMapSurface)

        assertNull(camera.mapboxCarMapSurface)
        assertFalse(camera.isLocationInitialized)
        verify { mapboxNavigation.unregisterLocationObserver(any()) }
    }
}
