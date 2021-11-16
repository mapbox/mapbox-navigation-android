package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * Interface definition for an observer that is notified whenever
 * the Navigation SDK checks for alternative routes to the destination change.
 *
 * @see [RouteAlternativesOptions] to control the callback interval.
 */
fun interface RouteAlternativesObserver {
    /**
     * Invoked whenever available alternative routes to the destination change.
     *
     * This callback if invoked whenever new alternatives are available (addition to the list),
     * or when a fork between an alternative and the current primary route has been passed (removal from the list).
     *
     * The [alternatives] list always represent all available, up-to-date, alternatives for the current route.
     *
     * The alternatives are not automatically added to [MapboxNavigation],
     * you need to add them manually to trigger [RoutesObserver], for example:
     * ```kotlin
     * registerRouteAlternativesObserver(
     *     RouteAlternativesObserver { routeProgress, alternatives, routerOrigin ->
     *         val newRoutes = mutableListOf<DirectionsRoute>().apply {
     *             add(mapboxNavigation.getRoutes().first())
     *             addAll(alternatives)
     *         }
     *         mapboxNavigation.setRoutes(newRoutes)
     *     }
     * )
     * ```
     *
     * You can filter the alternatives out before setting them back to [MapboxNavigation] based on requirements.
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
