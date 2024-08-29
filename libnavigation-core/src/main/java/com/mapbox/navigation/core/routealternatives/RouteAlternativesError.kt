package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Provides a reason for a route alternatives request failure.
 *
 * @param message message
 * @param routerOrigin the router type which failed to obtain alternatives
 * @param throwable cause
 */
class RouteAlternativesError internal constructor(
    val message: String,
    @RouterOrigin
    val routerOrigin: String? = null,
    val throwable: Throwable? = null,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlternativesError

        if (routerOrigin != other.routerOrigin) return false
        if (message != other.message) return false
        if (throwable != other.throwable) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routerOrigin.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + throwable.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlternativesError(" +
            "routerOrigin=$routerOrigin, " +
            "message='$message', " +
            "throwable=$throwable" +
            ")"
    }
}
