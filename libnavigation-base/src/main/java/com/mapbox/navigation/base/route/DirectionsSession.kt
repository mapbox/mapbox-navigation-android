package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route

interface DirectionsSession {
    fun getRoutes(): List<Route>
    fun getOrigin(): Point?
    fun getWaypoints(): List<Point>
    fun getDestination(): Point?
    // note: write in javadoc for java devs about default param waypoints
    fun requestRoutes(origin: Point, waypoints: List<Point> = emptyList(), destination: Point)
    fun cancel()

    interface RouteObserver {
        fun onRoutesChanged(routes: List<Route>)

        fun onRoutesRequested()

        fun onRoutesRequestFailure(throwable: Throwable)
    }
}
