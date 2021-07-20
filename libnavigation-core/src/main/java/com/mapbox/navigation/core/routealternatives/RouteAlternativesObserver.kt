package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Interface definition for an observer that is notified whenever
 * the Navigation SDK checks for alternative routes to the destination.
 *
 * @see [RouteAlternativesOptions] to control the callback interval.
 */
interface RouteAlternativesObserver {
    /**
     * Invoked whenever alternative routes are inspected. There are no available
     * alternatives if the list is empty.
     *
     * @param routeProgress the current route's progress.
     * @param alternatives list of alternative routes, can be empty.
     */
    fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<DirectionsRoute>,
        routerOrigin: RouterOrigin
    )
}
