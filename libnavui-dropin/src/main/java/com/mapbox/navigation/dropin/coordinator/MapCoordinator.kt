package com.mapbox.navigation.dropin.coordinator

import com.mapbox.maps.MapView
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.map.ActiveGuidanceMapBinder
import com.mapbox.navigation.dropin.binder.map.FreeDriveMapBinder
import com.mapbox.navigation.dropin.binder.map.RoutePreviewMapBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for the map.
 * This will include camera, location puck, and route line.
 */
internal class MapCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    mapView: MapView,
) : UICoordinator<MapView>(mapView) {

    // Temporarily flow to wire the map states
    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<MapView>> {
        return navigationViewContext.viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.Empty,
                NavigationState.FreeDrive -> FreeDriveMapBinder(navigationViewContext)
                NavigationState.RoutePreview -> RoutePreviewMapBinder(navigationViewContext)
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> ActiveGuidanceMapBinder(navigationViewContext)
            }
        }
    }
}
