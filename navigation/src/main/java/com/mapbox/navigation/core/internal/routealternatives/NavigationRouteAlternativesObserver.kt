package com.mapbox.navigation.core.internal.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError

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
     * private val alternativesObserver = object : NavigationRouteAlternativesObserver {
     *     override fun onRouteAlternatives(
     *         routeProgress: RouteProgress,
     *         alternatives: List<NavigationRoute>,
     *         @RouterOrigin routerOrigin: String
     *     ) {
     *         val newRoutes = mutableListOf<NavigationRoute>().apply {
     *             add(mapboxNavigation.getNavigationRoutes().first())
     *             addAll(alternatives)
     *         }
     *
     *         mapboxNavigation.setNavigationRoutes(newRoutes, mapboxNavigation.currentLegIndex())
     *     }
     *
     *     override fun onRouteAlternativesError(error: RouteAlternativesError) {
     *         // error handling
     *     }
     * }
     * mapboxNavigation.replaceDefaultAlternativeRoutesHandling(alternativesObserver)
     * ```
     * Please note that the example above doesn't properly handle offline-online route switching.
     * Consider it as a basic example for demonstration purpose.
     *
     * You can filter the alternatives out before setting them back to [MapboxNavigation] based on requirements.
     *
     * Additionally, you can use the alternatives observer to switch to an offboard route if the current route was built onboard
     * which can be used to always prefer an offboard-generated route over onboard-generated one.
     * This is a good practice because offboard-generated routes take live road conditions into account,
     * have more precise ETAs, and can also be refreshed as the user drives and conditions change. Example usage:
     * ```kotlin
     * private val alternativesObserver = object : NavigationRouteAlternativesObserver {
     *     override fun onRouteAlternatives(
     *         routeProgress: RouteProgress,
     *         alternatives: List<NavigationRoute>,
     *         @RouterOrigin routerOrigin: String
     *     ) {
     *         val primaryRoute = routeProgress.navigationRoute
     *         val isPrimaryRouteOffboard = primaryRoute.origin == RouterOrigin.Offboard
     *         val offboardAlternatives = alternatives.filter { it.origin == RouterOrigin.Offboard }
     *
     *         when {
     *             isPrimaryRouteOffboard -> {
     *                 // if the current route is offboard, keep it
     *                 // but consider accepting additional offboard alternatives only and ignore onboard ones
     *                 val updatedRoutes = mutableListOf<NavigationRoute>()
     *                 updatedRoutes.add(primaryRoute)
     *                 updatedRoutes.addAll(offboardAlternatives)
     *                 mapboxNavigation.setNavigationRoutes(updatedRoutes, mapboxNavigation.currentLegIndex())
     *             }
     *             isPrimaryRouteOffboard.not() && offboardAlternatives.isNotEmpty() -> {
     *                 // if the current route is onboard, and there's an offboard route available
     *                 // consider notifying the user that a more accurate route is available and whether they'd want to switch
     *                 // or force the switch like presented
     *                 mapboxNavigation.setNavigationRoutes(offboardAlternatives)
     *             }
     *             else -> {
     *                 // in other cases, when current route is onboard and there are no offboard alternatives,
     *                 // just append the new alternatives
     *                 val updatedRoutes = mutableListOf<NavigationRoute>()
     *                 updatedRoutes.add(primaryRoute)
     *                 updatedRoutes.addAll(alternatives)
     *                 mapboxNavigation.setNavigationRoutes(updatedRoutes, mapboxNavigation.currentLegIndex())
     *             }
     *         }
     *     }
     *
     *     override fun onRouteAlternativesError(error: RouteAlternativesError) {
     *         // error handling
     *     }
     * }
     * mapboxNavigation.replaceDefaultAlternativeRoutesHandling(alternativesObserver)
     * ```
     *
     * @param routeProgress the current route's progress.
     * @param alternatives list of alternative routes, can be empty.
     * @param routerOrigin reports the source of all the new alternative routes in the list.
     * If there are no new routes, reports the source that returned the latest additions.
     */
    fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        @RouterOrigin
        routerOrigin: String,
    )

    /**
     * Notified when an alternative routes request encounters an error.
     */
    fun onRouteAlternativesError(error: RouteAlternativesError)
}
