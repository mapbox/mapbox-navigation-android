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
     * Invoked whenever a list of routes changes and immediately when registered
     * (see [MapboxNavigation.registerRoutesObserver]) if non-empty routes are present.
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
 * @param ignoredRoutes list of alternative routes that were ignored
 *  because they are invalid for navigation. See [IgnoredRoute] for details.
 * @param reason why the routes have been updated (re-route, refresh route, and etc.)
 */
class RoutesUpdatedResult internal constructor(
    val navigationRoutes: List<NavigationRoute>,
    val ignoredRoutes: List<IgnoredRoute>,
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

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesUpdatedResult

        if (navigationRoutes != other.navigationRoutes) return false
        if (reason != other.reason) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = navigationRoutes.hashCode()
        result = 31 * result + reason.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesUpdatedResult(" +
            "reason='$reason', " +
            "navigationRoutes=$navigationRoutes, " +
            "ignoredRoutes=$ignoredRoutes" +
            ")"
    }
}

/**
 * Model class that contains info about ignored routes.
 *
 * @param navigationRoute route that was ignored because it is invalid for navigation
 * @param reason reason why the route was ignored
 */
class IgnoredRoute internal constructor(
    val navigationRoute: NavigationRoute,
    val reason: String,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IgnoredRoute(navigationRoute=$navigationRoute, reason='$reason')"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IgnoredRoute

        if (navigationRoute != other.navigationRoute) return false
        if (reason != other.reason) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = navigationRoute.hashCode()
        result = 31 * result + reason.hashCode()
        return result
    }
}
