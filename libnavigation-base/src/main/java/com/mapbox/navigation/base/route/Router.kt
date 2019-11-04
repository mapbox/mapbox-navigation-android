package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route

interface Router {

    fun getRoute(
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        listener: RouteListener
    )

    fun cancel()

    interface RouteListener {
        fun onRouteReady(route: Route)

        fun onFailure(throwable: Throwable)
    }
}
