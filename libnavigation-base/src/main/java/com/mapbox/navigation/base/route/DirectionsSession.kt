package com.mapbox.navigation.base.route

import android.location.Location
import com.mapbox.navigation.base.route.model.Route

interface DirectionsSession {
    var currentRoute: Route?

    fun setOrigin(point: Location)
    fun getOrigin(): Location
    fun setWaypoints(points: List<Location>)
    fun getWaypoints(): List<Location>

    fun registerRouteObserver(routeObserver: RouteObserver)
    fun unregisterRouteObserver(routeObserver: RouteObserver)
    fun cancel()

    interface RouteObserver {
        fun onRouteChanged(route: Route?)

        fun onFailure(throwable: Throwable)
    }
}
