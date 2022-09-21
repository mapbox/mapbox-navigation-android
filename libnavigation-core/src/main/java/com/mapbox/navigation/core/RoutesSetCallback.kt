package com.mapbox.navigation.core

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Interface definition for a callback that gets notified whenever routes
 * passed to [MapboxNavigation.setNavigationRoutes] are set or produce an error and ignored.
 */
fun interface RoutesSetCallback {

    /**
     * Invoked on result of [MapboxNavigation.setNavigationRoutes].
     *
     * @param result [Expected] object with [RoutesSetError] in case of a failed
     *   and [RoutesSetSuccess] in case of a successful
     *   [MapboxNavigation.setNavigationRoutes] invocation.
     */
    fun onRoutesSet(result: Expected<RoutesSetError, RoutesSetSuccess>)
}

/**
 * Result when the primary route has been successfully set and we can begin Active Guidance.
 *
 * @param ignoredAlternatives alternative routes that were ignored by the navigator.
 *   Key is the [NavigationRoute.id], [RoutesSetError] value describes the error.
 */
class RoutesSetSuccess internal constructor(
    val ignoredAlternatives: Map<String, RoutesSetError>,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesSetSuccess(ignoredAlternatives=$ignoredAlternatives)"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesSetSuccess

        if (ignoredAlternatives != other.ignoredAlternatives) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return ignoredAlternatives.hashCode()
    }
}

/**
 * Result when a route failed to be set.
 *
 * @param message a string describing the reason why the route was ignored.
 */
class RoutesSetError internal constructor(
    val message: String,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesSetError(message='$message')"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesSetError

        if (message != other.message) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return message.hashCode()
    }
}
