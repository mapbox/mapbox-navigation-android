package com.mapbox.navigation

import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.directions.session.DirectionsSession
import com.mapbox.navigation.directions.session.MapboxDirectionsSession

internal object NavigationComponentProvider {
    fun createDirectionsSession(router: Router, routeObserver: DirectionsSession.RouteObserver) =
        MapboxDirectionsSession(router, routeObserver)
}
