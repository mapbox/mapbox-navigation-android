package com.mapbox.navigation.testing.utils.assertions

import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.ui.assertions.ValueTransitionAssertion

class RouteProgressStateTransitionAssertion(
    mapboxNavigation: MapboxNavigation,
    expectedBlock: ValueTransitionAssertion<RouteProgressState>.() -> Unit
) : ValueTransitionAssertion<RouteProgressState>(expectedBlock) {
    init {
        mapboxNavigation.registerRouteProgressObserver { routeProgress ->
            onNewValue(routeProgress.currentState)
        }
    }
}
