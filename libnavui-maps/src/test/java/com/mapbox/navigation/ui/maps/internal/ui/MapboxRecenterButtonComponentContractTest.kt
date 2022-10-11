package com.mapbox.navigation.ui.maps.internal.ui

import android.location.Location
import android.location.LocationManager
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.RecenterButtonConfig
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class MapboxRecenterButtonComponentContractTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var cameraPlugin: CameraAnimationsPlugin
    private lateinit var componentConfig: RecenterButtonConfig

    private lateinit var sut: MapboxRecenterButtonComponentContract

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        cameraPlugin = mockk(relaxed = true)
        val mapView: MapView = mockk {
            every { camera } returns cameraPlugin
        }
        componentConfig = RecenterButtonConfig().apply {
            cameraOptions = CameraOptions.Builder().zoom(4.0).build()
            animationOptions = MapAnimationOptions.mapAnimationOptions {
                duration(2000L)
            }
        }

        sut = MapboxRecenterButtonComponentContract(mapView, componentConfig)
    }

    @Test
    fun `onClick should recenter camera to last know location`() {
        val loc = Location(LocationManager.PASSIVE_PROVIDER).apply {
            latitude = 1.0
            longitude = 2.0
        }
        givenLocationUpdate(loc)
        sut.onAttached(mapboxNavigation)

        sut.onClick(mockk())

        val cameraOptions = slot<CameraOptions>()
        verify { cameraPlugin.easeTo(capture(cameraOptions), any()) }
        assertEquals(componentConfig.cameraOptions.zoom, cameraOptions.captured.zoom)
        assertEquals(loc.toPoint(), cameraOptions.captured.center)
    }

    @Test
    fun `onClick should use animationOptions from componentConfig`() {
        val loc = Location(LocationManager.PASSIVE_PROVIDER).apply {
            latitude = 1.0
            longitude = 2.0
        }
        givenLocationUpdate(loc)
        sut.onAttached(mapboxNavigation)

        sut.onClick(mockk())

        val animOptions = slot<MapAnimationOptions>()
        verify { cameraPlugin.easeTo(any(), capture(animOptions)) }
        assertEquals(componentConfig.animationOptions, animOptions.captured)
    }

    private fun givenLocationUpdate(location: Location) {
        val result = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns location
        }
        val locObserver = slot<LocationObserver>()
        every {
            mapboxNavigation.registerLocationObserver(capture(locObserver))
        } answers {
            locObserver.captured.onNewRawLocation(location)
            locObserver.captured.onNewLocationMatcherResult(result)
        }
    }
}
