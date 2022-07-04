package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Interface definition for a callback that gets notified whenever routes
 * passed to [MapboxNavigation.setNavigationRoutes] are set or produce an error and ignored.
 */
interface RoutesSetCallback {

    /**
     * Invoked whenever the routes passed to [MapboxNavigation.setNavigationRoutes]
     * are successfully set.
     *
     * @param result [RoutesSetCallbackSuccess] object that describes the set routes
     */
    fun onRoutesSetResult(result: RoutesSetCallbackSuccess)

    /**
     * Invoked whenever the routes passed to [MapboxNavigation.setNavigationRoutes]
     * produce an error and are ignored.
     *
     * @param result [RoutesSetCallbackError] object that describes the failure.
     */
    fun onRoutesSetError(result: RoutesSetCallbackError)
}

/**
 * Represents the result of setting routes. See [RoutesSetCallback.onRoutesSetResult].
 *
 * @param routes List of routes that were successfully set.
 */
class RoutesSetCallbackSuccess internal constructor(
    val routes: List<NavigationRoute>
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesSetCallbackSuccess

        if (routes != other.routes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return routes.hashCode()
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RoutesSetCallbackSuccess(routes=$routes)"
    }
}

/**
 * Represents the error of setting routes. See [RoutesSetCallback.onRoutesSetError].
 *
 * @param routes List of routes that were ignored.
 * @param error Reason why routes were ignored.
 */
class RoutesSetCallbackError internal constructor(
    val routes: List<NavigationRoute>,
    val error: String
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesSetCallbackError

        if (routes != other.routes) return false
        if (error != other.error) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = routes.hashCode()
        result = 31 * result + error.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RoutesSetCallbackFailure(routes=$routes, error=$error)"
    }
}
