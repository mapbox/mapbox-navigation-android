package com.mapbox.navigation.route

import com.mapbox.geojson.Point

interface DirectionsSession {
    var origin: Point
    var waypoints: List<Point>
    var currentRoute: Route?

    fun registerRouteObserver(routeObserver: RouteObserver)
    fun unregisterRouteObserver(routeObserver: RouteObserver)
    fun cancel()

    interface RouteObserver {
        fun onRouteChanged(route: Route?)
    }
}
