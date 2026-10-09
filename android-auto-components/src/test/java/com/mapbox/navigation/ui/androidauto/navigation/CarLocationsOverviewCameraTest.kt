package com.mapbox.navigation.ui.androidauto.navigation

import android.content.res.Configuration
import android.graphics.Rect
import androidx.car.app.CarContext
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
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
import org.junit.Assert.assertEquals
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
        every { mapboxMap.isValid() } returns true
        every { mapboxMap.subscribeStyleLoaded(any()) } returns mockk(relaxed = true)
        val cameraAnimationsPlugin = mockk<CameraAnimationsPlugin>()
        val aMapSurface = mockk<MapSurface> {
            every { getMapboxMap() } returns mapboxMap
            every { camera } returns cameraAnimationsPlugin
        }
        val mapboxCarMapSurface = mockk<MapboxCarMapSurface> {
            every { mapSurface } returns aMapSurface
            every { carContext } returns aCarContext(densityDpi = 160)
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

    @Test
    fun `overview padding is converted from dp to pixels`() {
        val mapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
        }
        val aMapSurface = mockk<MapSurface> {
            every { getMapboxMap() } returns mapboxMap
            every { camera } returns mockk()
        }
        val mapboxCarMapSurface = mockk<MapboxCarMapSurface> {
            every { mapSurface } returns aMapSurface
            every { carContext } returns aCarContext(densityDpi = 320)
        }
        val camera = CarLocationsOverviewCamera()

        carAppTestRule.onAttached(mockk(relaxUnitFun = true))
        camera.onAttached(mapboxCarMapSurface)
        camera.onVisibleAreaChanged(Rect(), EdgeInsets(10.0, 20.0, 30.0, 40.0))

        assertEquals(
            EdgeInsets(40.0, 50.0, 60.0, 70.0),
            camera.viewportDataSource.overviewPadding,
        )
    }

    private fun aCarContext(densityDpi: Int): CarContext = mockk {
        every { resources } returns mockk {
            every { configuration } returns Configuration().also { it.densityDpi = densityDpi }
        }
    }
}
