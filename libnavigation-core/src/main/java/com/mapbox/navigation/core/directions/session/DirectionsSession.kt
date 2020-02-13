package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver

internal interface DirectionsSession {

    var routes: List<DirectionsRoute>

    fun getRouteOptions(): RouteOptions?

    fun requestRoutes(routeOptions: RouteOptions, routesRequestCallback: RoutesRequestCallback)

    fun cancel()

    fun registerRoutesObserver(routesObserver: RoutesObserver)

    fun unregisterRoutesObserver(routesObserver: RoutesObserver)

    fun registerFasterRouteObserver(fasterRouteObserver: FasterRouteObserver)

    fun unregisterFasterRouteObserver(fasterRouteObserver: FasterRouteObserver)

    fun shutDownSession()

    fun unregisterAllRoutesObservers()
}
