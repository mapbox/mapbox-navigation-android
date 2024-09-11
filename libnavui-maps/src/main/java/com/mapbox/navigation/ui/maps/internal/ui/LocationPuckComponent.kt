package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

class LocationPuckComponent(
    private val locationComponentPlugin: LocationComponentPlugin,
    private val locationPuck: LocationPuck,
    private val locationProvider: NavigationLocationProvider,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        locationComponentPlugin.apply {
            if (getLocationProvider() != locationProvider) {
                setLocationProvider(locationProvider)
            }
            locationPuck = this@LocationPuckComponent.locationPuck
            puckBearingEnabled = true
            enabled = true
        }
    }
}
