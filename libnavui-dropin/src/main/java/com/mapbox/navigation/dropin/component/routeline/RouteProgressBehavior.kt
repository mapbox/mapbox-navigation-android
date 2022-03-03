package com.mapbox.navigation.dropin.component.routeline

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteProgressBehavior : MapboxNavigationObserver {

    private val _routeProgressStateFlow = MutableStateFlow<RouteProgress?>(null)
    val routeProgressStateFlow = _routeProgressStateFlow.asStateFlow()

    private val routeProgressObserver = RouteProgressObserver {
        _routeProgressStateFlow.value = it
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }
}
