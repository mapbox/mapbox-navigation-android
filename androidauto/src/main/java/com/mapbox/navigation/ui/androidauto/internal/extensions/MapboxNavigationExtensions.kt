@file:JvmName("MapboxNavigationEx")

package com.mapbox.navigation.ui.androidauto.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

/**
 * This extension removes boilerplate from a class that needs to use [MapboxNavigation], and it
 * makes your class appear as if it implements [MapboxNavigationObserver] without exposing the
 * functions.
 *
 * ``` kotlin
 * val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)
 * private fun onAttached(mapboxNavigation: MapboxNavigation)
 * private fun onDetached(mapboxNavigation: MapboxNavigation)
 * ```
 */
fun mapboxNavigationForward(
    attach: (MapboxNavigation) -> Unit,
    detach: (MapboxNavigation) -> Unit,
) = object : MapboxNavigationObserver {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        attach(mapboxNavigation)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        detach(mapboxNavigation)
    }
}

/**
 * Starts active guidance on the previewed route with [routeId], keeping the other previewed
 * routes as alternatives, and clears the preview. The route is looked up by id in the current
 * preview, so a click from a template built for an older preview can't start a route the driver
 * didn't pick.
 *
 * [MapboxNavigation.changeRoutesPreviewPrimaryRoute] applies asynchronously, so a selection
 * followed by [MapboxNavigation.moveRoutesFromPreviewToNavigator] could still start the previous
 * primary route. The reordered routes are set on the navigator directly instead.
 *
 * @return false when there is no preview or the route is no longer previewed.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun MapboxNavigation.startGuidanceOnPreviewedRoute(routeId: String): Boolean {
    val originalRoutes = getRoutesPreview()?.originalRoutesList ?: return false
    val selectedRoute = originalRoutes.firstOrNull { it.id == routeId } ?: return false
    setNavigationRoutes(listOf(selectedRoute) + originalRoutes.filter { it != selectedRoute })
    setRoutesPreview(emptyList())
    return true
}

/**
 * Makes the previewed route with [routeId] the primary previewed route.
 *
 * @return false when there is no preview or the route is no longer previewed.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun MapboxNavigation.selectPreviewedRoute(routeId: String): Boolean {
    val route = getRoutesPreview()?.originalRoutesList?.firstOrNull { it.id == routeId }
        ?: return false
    changeRoutesPreviewPrimaryRoute(route)
    return true
}
