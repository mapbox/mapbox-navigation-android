package com.mapbox.navigation.core.directions.session

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress

internal interface DirectionsSession {

    var routes: List<DirectionsRoute>

    fun getRouteOptions(): RouteOptions?

    fun getAdjustedRouteOptions(
        routeOptions: RouteOptions,
        routeProgress: RouteProgress,
        location: Location
    ): RouteOptions

    fun requestRoutes(routeOptions: RouteOptions, routesRequestCallback: RoutesRequestCallback? = null)

    /**
     * Requests a route using the provided [Router] implementation.
     * Unlike [DirectionsSession.requestRoutes] it ignores the result and it's up to the
     * consumer to take an action with the route.
     *
     * @param adjustedRouteOptions: RouteOptions with adjusted parameters
     * @param routesRequestCallback listener that gets notified when request state changes
     */
    fun requestFasterRoute(adjustedRouteOptions: RouteOptions, routesRequestCallback: RoutesRequestCallback)

    fun cancel()

    fun registerRoutesObserver(routesObserver: RoutesObserver)

    fun unregisterRoutesObserver(routesObserver: RoutesObserver)

    fun shutDownSession()

    fun unregisterAllRoutesObservers()
}
