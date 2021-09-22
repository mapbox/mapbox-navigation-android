package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.RouteAlternativesOptions
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
     *
     * @return list of indexes that should no longer be considered alternatives.
     */
    fun onRouteAlternatives(
        routeProgress: RouteProgress?,
        alternatives: List<RouteAlternative>
    )
}
