package com.mapbox.navigation.dropin.component.routearrow

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal sealed interface RouteArrowUIComponent : UIComponent

internal class MapboxRouteArrowUIComponent(
    val view: MapView,
    val viewModel: RouteArrowViewModel
) : RouteArrowUIComponent, RouteProgressObserver {

    override fun onNavigationStateChanged(state: NavigationState) {
        // no impl
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        view.getMapboxMap().getStyle()?.let { style ->
            viewModel.routeProgressUpdated(routeProgress, style)
        }
    }
}
