package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
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
interface NavigationRouteAlternativesObserver {
    /**
     * Invoked whenever available alternative routes to the destination change.
     *
     * This callback is invoked whenever new alternatives are available (addition to the list),
     * or when a fork between an alternative and the current primary route has been passed (removal from the list).
     *
     * The [alternatives] list always represent all available, up-to-date, alternatives for the current route.
     *
     * The alternatives are not automatically added to [MapboxNavigation],
     * you need to add them manually to trigger [RoutesObserver], for example:
     * ```kotlin
     * mapboxNavigation.requestAlternativeRoutes(
     *     object : NavigationRouteAlternativesRequestCallback {
     *         override fun onRouteAlternativeRequestFinished(
     *             routeProgress: RouteProgress,
     *             alternatives: List<NavigationRoute>,
     *             routerOrigin: RouterOrigin
     *         ) {
     *             val newRoutes = mutableListOf<NavigationRoute>().apply {
     *                 add(mapboxNavigation.getNavigationRoutes().first())
     *                 addAll(alternatives)
     *             }
     *             mapboxNavigation.setNavigationRoutes(newRoutes)
     *         }
     *         override fun onRouteAlternativesRequestError(error: RouteAlternativesError) {
     *             // ...
     *         }
     *     }
     * )
     * ```
     *
     * You can filter the alternatives out before setting them back to [MapboxNavigation] based on requirements.
     *
     * @param routeProgress the current route's progress.
     * @param alternatives list of alternative routes, can be empty.
     * @param routerOrigin reports the source of all the new alternative routes in the list.
     * If there are no new routes, reports the source that returned the latest additions.
     */
    fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        routerOrigin: RouterOrigin
    )

    /**
     * Notified when an alternative routes request encounters an error.
     */
    fun onRouteAlternativesError(error: RouteAlternativesError)
}
