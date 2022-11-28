package com.mapbox.navigation.instrumentation_tests.utils.assertions

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.ui.assertions.ValueTransitionAssertion

class RouteProgressStateTransitionAssertion(
    mapboxNavigation: MapboxNavigation,
    expectedBlock: ValueTransitionAssertion<RouteProgressState>.() -> Unit
) : ValueTransitionAssertion<RouteProgressState>(expectedBlock) {
    init {
        mapboxNavigation.registerRouteProgressObserver(
            object : RouteProgressObserver {
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    onNewValue(routeProgress.currentState)
                }
            }
        )
    }
}
