package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

internal class LocationPuckComponentTest {

    private lateinit var sut: LocationPuckComponent

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var locationProvider: NavigationLocationProvider
    private lateinit var locationPlugin: LocationComponentPlugin
    private lateinit var locationPuck: LocationPuck

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        locationPuck = mockk(relaxed = true)
        locationPlugin = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)

        sut = LocationPuckComponent(locationPlugin, locationPuck, locationProvider)
    }

    @Test
    fun `onAttach should configure and enable location component`() {
        sut.onAttached(mapboxNavigation)

        verify { locationPlugin.setLocationProvider(locationProvider) }
        verify { locationPlugin.enabled = true }
    }

    @Test
    fun `onAttach should configure location puck`() {
        sut.onAttached(mapboxNavigation)

        verify { locationPlugin.locationPuck = locationPuck }
    }
}
