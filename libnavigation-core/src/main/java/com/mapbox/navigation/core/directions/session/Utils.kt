package com.mapbox.navigation.core.directions.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue

internal object Utils {

    private const val INVALID_ROUTE_REASON = "Route is invalid for navigation"

    fun createDirectionsSessionRoutes(
        inputRoutes: List<NavigationRoute>,
        processedRoutes: NativeSetRouteValue,
        setRoutesInfo: SetRoutes,
    ): DirectionsSessionRoutes {
        val (acceptedAlternatives, ignoredAlternatives) = inputRoutes
            .drop(1)
            .partition { passedRoute ->
                processedRoutes.nativeAlternatives.any { processedRoute ->
                    processedRoute.route.routeId == passedRoute.id
                }
            }
        val validRoutes = listOfNotNull(processedRoutes.routes.firstOrNull()) +
            acceptedAlternatives
        return DirectionsSessionRoutes(
            validRoutes,
            ignoredAlternatives.map { IgnoredRoute(it, INVALID_ROUTE_REASON) },
            setRoutesInfo,
        )
    }
}
