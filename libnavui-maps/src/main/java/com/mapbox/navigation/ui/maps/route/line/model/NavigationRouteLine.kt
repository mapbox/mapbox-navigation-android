@file:JvmName("NavigationRouteLineEx")

package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute

/**
 * Represents a route and an optional identification of used for representing routes on the map.
 *
 * @param route a directions route
 * @param identifier an optional identifier for the directions route which can be used to
 * influence color of the route when it is an alternative route.
 */
data class NavigationRouteLine(val route: NavigationRoute, val identifier: String?)

/**
 * Maps [RouteLine] to [NavigationRouteLine].
 */
fun RouteLine.toNavigationRouteLine() = NavigationRouteLine(
    route.toNavigationRoute(),
    identifier
)

/**
 * Maps [RouteLine]s to [NavigationRouteLine]s.
 */
fun List<RouteLine>.toNavigationRouteLines() = map { it.toNavigationRouteLine() }
