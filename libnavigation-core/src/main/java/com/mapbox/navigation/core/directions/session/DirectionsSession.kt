package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

internal interface DirectionsSession {

    fun getRoutes(): List<DirectionsRoute>

    fun getRouteOptions(): RouteOptions?

    fun requestRoutes(routeOptions: RouteOptions)

    fun cancel()

    fun registerRouteObserver(routeObserver: RouteObserver)

    fun unregisterRouteObserver(routeObserver: RouteObserver)

    fun shutDownSession()
}
