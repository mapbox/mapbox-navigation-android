package com.mapbox.navigation.ui.maps.internal.ui

import android.location.Location
import android.location.LocationManager
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class LocationPuckComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: LocationPuckComponent

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var locationProvider: NavigationLocationProvider
    private lateinit var locationPlugin: LocationComponentPlugin
    private lateinit var locationPuck: LocationPuck
    private lateinit var map: MapboxMap

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        locationPuck = mockk(relaxed = true)
        locationPlugin = mockk(relaxed = true)
        locationProvider = spyk(NavigationLocationProvider())
        map = mockk {
            val slot = slot<Style.OnStyleLoaded>()
            every { getStyle(capture(slot)) } answers {
                slot.captured.onStyleLoaded(mockk())
            }
        }

        sut = LocationPuckComponent(map, locationPlugin, locationPuck, locationProvider)
    }

    @Test
    fun `onAttach should configure and enable location component`() = runBlockingTest {
        givenLocationUpdate(location(1.0, 2.0))
        sut.onAttached(mapboxNavigation)

        verify { locationPlugin.setLocationProvider(locationProvider) }
        verify { locationPlugin.enabled = true }
    }

    @Test
    fun `onAttach should configure location puck`() = runBlockingTest {
        givenLocationUpdate(location(2.0, 3.0))
        sut.onAttached(mapboxNavigation)

        verify { locationPlugin.locationPuck = locationPuck }
    }

    private fun givenLocationUpdate(location: Location) {
        locationProvider.changePosition(location)
    }

    private fun location(latitude: Double, longitude: Double) = Location(
        LocationManager.PASSIVE_PROVIDER
    ).apply {
        this.latitude = latitude
        this.longitude = longitude
    }
}
