package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route

interface DirectionsSession {
    fun getRoutes(): List<Route>
    fun setOrigin(point: Point)
    fun getOrigin(): Point
    fun setWaypoints(points: List<Point>)
    fun getWaypoints(): List<Point>
    fun setDestination(point: Point)
    fun getDestination(): Point
    fun requestRoutes()
    fun cancel()

    interface RouteObserver {
        fun onRoutesChanged(routes: List<Route>)

        fun onRoutesRequested()

        fun onRoutesRequestFailure(throwable: Throwable)
    }
}
