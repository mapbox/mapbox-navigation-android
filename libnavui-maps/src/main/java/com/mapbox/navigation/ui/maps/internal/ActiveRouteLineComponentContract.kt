package com.mapbox.navigation.ui.maps.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
class ActiveRouteLineComponentContract : RouteLineComponentContract, MapboxNavigationObserver {
    private val _routesFlow = MutableStateFlow<List<NavigationRoute>>(emptyList())
    private val observer = RoutesObserver { _routesFlow.value = it.navigationRoutes }

    override val navigationRoutes: StateFlow<List<NavigationRoute>> = _routesFlow

    override fun setRoutes(routes: List<NavigationRoute>) {
        _routesFlow.value = routes
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutesObserver(observer)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(observer)
    }
}
