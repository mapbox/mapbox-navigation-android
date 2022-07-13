package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Interface definition for an observer that gets notified whenever a list of maintained routes changes.
 */
fun interface RoutesObserver {

    /**
     * Invoked whenever a list of routes changes.
     *
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance'.
     *
     * A list of routes can be modified internally and externally at any time with
     * [MapboxNavigation.setRoutes], or during automatic reroutes, faster route and route refresh operations.
     */
    fun onRoutesChanged(result: RoutesUpdatedResult)
}

/**
 * Routes updated result is provided via [RoutesObserver] whenever a list of routes changes.
 *
 * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance'.
 *
 * @param navigationRoutes list of currently maintained routes
 * @param reason why the routes have been updated (re-route, refresh route, and etc.)
 */
class RoutesUpdatedResult internal constructor(
    val navigationRoutes: List<NavigationRoute>,
    @RoutesExtra.RoutesUpdateReason val reason: String,
) {
    /**
     * List of currently maintained routes.
     *
     * @throws IllegalStateException see [toDirectionsRoutes]
     */
    @Deprecated(
        "use #navigationRoutes instead",
        ReplaceWith(
            "navigationRoutes.toDirectionsRoutes()",
            "com.mapbox.navigation.base.route.toDirectionsRoutes"
        )
    )
    val routes: List<DirectionsRoute> by lazy { navigationRoutes.toDirectionsRoutes() }
}
