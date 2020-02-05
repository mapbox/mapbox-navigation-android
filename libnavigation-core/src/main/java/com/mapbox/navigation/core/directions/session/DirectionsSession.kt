package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

internal interface DirectionsSession {

    var routes: List<DirectionsRoute>

    fun getRouteOptions(): RouteOptions?

    fun requestRoutes(routeOptions: RouteOptions, routesRequestCallback: RoutesRequestCallback)

    fun cancel()

    fun registerRoutesObserver(routesObserver: RoutesObserver)

    fun unregisterRoutesObserver(routesObserver: RoutesObserver)

    fun shutDownSession()

    fun unregisterAllRoutesObservers()
}
