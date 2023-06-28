package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Interface definition for a callback associated with routes refresh.
 */
interface NavigationRouterRefreshCallback {
    /**
     * Called with a new instance of [NavigationRoute] with the underlying [DirectionsRoute] refreshed.
     */
    fun onRefreshReady(route: NavigationRoute)

    /**
     * Called when an error has occurred while refreshing the route.
     *
     * @see RouterFactory.buildNavigationRouterRefreshError
     */
    fun onFailure(error: NavigationRouterRefreshError)
}

/**
 * Provides information about the route refresh failure.
 *
 * @param message message
 * @param throwable cause
 * @param routerFailure failure in case the failure happened during a request
 * @param refreshTtl time in seconds after which the route will be invalidated
 *  (no refreshes will be possible). Baseline is request time.
 */
class NavigationRouterRefreshError internal constructor(
    val message: String? = null,
    val throwable: Throwable? = null,
    val routerFailure: RouterFailure? = null,
    val refreshTtl: Int? = null,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationRouterRefreshError

        if (message != other.message) return false
        if (throwable != other.throwable) return false
        if (routerFailure != other.routerFailure) return false
        if (refreshTtl != other.refreshTtl) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + throwable.hashCode()
        result = 31 * result + routerFailure.hashCode()
        result = 31 * result + refreshTtl.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationRouterRefreshError(" +
            "message=$message, " +
            "throwable=$throwable, " +
            "routerFailure=$routerFailure, " +
            "refreshTtl=$refreshTtl" +
            ")"
    }
}
