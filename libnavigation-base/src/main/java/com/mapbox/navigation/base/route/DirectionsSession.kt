package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation

interface DirectionsSession {
    fun getRoutes(): List<Route>
    fun getRouteOptions(): RouteOptionsNavigation?
    fun requestRoutes(routeOptions: RouteOptionsNavigation)
    fun cancel()

    interface RouteObserver {
        fun onRoutesChanged(routes: List<Route>)

        fun onRoutesRequested()

        fun onRoutesRequestFailure(throwable: Throwable)
    }
}
