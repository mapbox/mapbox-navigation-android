package com.mapbox.navigation.core.internal.router

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure

/**
 * Interface definition for a callback associated with routes refresh.
 */
internal interface NavigationRouterRefreshCallback {
    /**
     * Called with a new instance of [NavigationRoute] with the underlying [DirectionsRoute] refreshed.
     * @param route The refreshed route
     * @param refreshResponse The original refresh response
     */
    fun onRefreshReady(route: NavigationRoute, refreshResponse: DataRef)

    /**
     * Called when an error has occurred while refreshing the route.
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
        return refreshTtl == other.refreshTtl
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
