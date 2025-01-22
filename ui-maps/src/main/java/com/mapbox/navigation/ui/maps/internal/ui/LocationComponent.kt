package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

class LocationComponent(
    val locationProvider: NavigationLocationProvider,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult().observe {
            locationProvider.changePosition(
                location = it.enhancedLocation,
                keyPoints = it.keyPoints,
            )
        }
    }
}
