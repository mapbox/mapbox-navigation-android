package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point

interface Router {

    @Deprecated("use [getRoute(Point, List<Point>, RouteListener)] instead")
    fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit)

    // fun getRoute(origin: Point, waypoints: List<Point>, listener: RouteListener)

    fun cancel()

    interface RouteListener {
        fun onRouteReady(route: Route)
    }
}

