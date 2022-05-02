@file:Suppress("NoMockkVerifyImport")
package com.mapbox.androidauto.car.navigation

import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.common.Logger
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before

import org.junit.Test

@OptIn(MapboxExperimental::class)
class CarLocationsOverviewCameraTest : MapboxRobolectricTestRunner() {

    @Before
    fun setup() {
        mockkStatic(Logger::class)
        every { Logger.e(any(), any()) } just Runs
        every { Logger.i(any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkStatic(Logger::class)
    }

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
        val camera = CarLocationsOverviewCamera(mapboxNavigation)

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
        val camera = CarLocationsOverviewCamera(mapboxNavigation)

        camera.onDetached(mapboxCarMapSurface)

        assertNull(camera.mapboxCarMapSurface)
        assertFalse(camera.isLocationInitialized)
        verify { mapboxNavigation.unregisterLocationObserver(any()) }
    }
}
