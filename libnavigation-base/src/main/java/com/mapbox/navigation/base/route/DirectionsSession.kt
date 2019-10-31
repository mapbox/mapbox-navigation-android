package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.base.route.model.Route

interface DirectionsSession {
    var origin: PointNavigation
    var waypoints: List<PointNavigation>
    var currentRoute: Route?

    fun registerRouteObserver(routeObserver: RouteObserver)
    fun unregisterRouteObserver(routeObserver: RouteObserver)
    fun cancel()

    interface RouteObserver {
        fun onRouteChanged(route: Route?)
    }
}
