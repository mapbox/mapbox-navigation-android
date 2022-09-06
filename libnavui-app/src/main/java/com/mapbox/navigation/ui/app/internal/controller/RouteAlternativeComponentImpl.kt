package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.maps.internal.ui.RouteAlternativeContract

class RouteAlternativeComponentImpl(private val store: Store) : RouteAlternativeContract {

    override fun onAlternativeRoutesUpdated(
        legIndex: Int,
        mapboxNavigation: MapboxNavigation,
        updatedRoutes: List<NavigationRoute>
    ) {
        store.dispatch(RoutesAction.SetRoutes(routes = updatedRoutes, legIndex = legIndex))
    }
}
