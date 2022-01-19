package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * Interface definition for a callback that is notified whenever alternatives routes are refreshed on demand.
 *
 * See [MapboxNavigation.requestAlternativeRoutes].
 */
interface NavigationRouteAlternativesRequestCallback {

    /**
     * Invoked when on-demand alternative routes request finishes.
     *
     * The [alternatives] list always represent all available, up-to-date, alternatives for the current route.
     *
     * The alternatives are not automatically added to [MapboxNavigation],
     * you need to add them manually to trigger [RoutesObserver], for example:
     * todo update docs (no fun int)
     * ```kotlin
     * mapboxNavigation.requestAlternativeRoutes(
     *     NavigationRouteAlternativesRequestListener { routeProgress, alternatives, routerOrigin ->
     *         val newRoutes = mutableListOf<NavigationRoute>().apply {
     *             add(mapboxNavigation.getNavigationRoutes().first())
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
     * @param routerOrigin reports the source of all the new alternative routes in the list.
     * If there are no new routes, reports the source that returned the latest additions.
     */
    fun onRouteAlternativeRequestFinished(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        routerOrigin: RouterOrigin
    )

    /**
     * Invoked when the request fails or is canceled.
     */
    fun onRouteAlternativesRequestError(error: RouteAlternativesError)
}
