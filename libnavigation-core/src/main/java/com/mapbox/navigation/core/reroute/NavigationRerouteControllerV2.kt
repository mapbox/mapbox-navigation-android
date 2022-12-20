package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Reroute controller allows changing the reroute logic externally. Use [MapboxNavigation.rerouteController]
 * to replace it.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface NavigationRerouteControllerV2 : NavigationRerouteController {
    /**
     * Invoked whenever re-route is needed. For instance when a driver is off-route. Called just after
     * an off-route event.
     *
     * @see [OffRouteObserver]
     */
    fun reroute(params: RerouteParameters, callback: NavigationRerouteController.RoutesCallback)
}

/***
 * Contains additional data which may be needed for the reroute logic.
 * @param detectedAlternative is an alternative route to which user has deviated from primary route. It's null if user deviated from primary route and doesn't follow existing alternatives.
 * @param routes current routes
 */
@ExperimentalPreviewMapboxNavigationAPI
class RerouteParameters internal constructor(
    val detectedAlternative: NavigationRoute?,
    val routes: List<NavigationRoute>,
) {
    companion object {
        internal fun create(
            routes: List<NavigationRoute>,
            detectedAlternativeId: String?
        ): RerouteParameters = RerouteParameters(
            routes.firstOrNull { it.id == detectedAlternativeId },
            routes
        )
    }
}
