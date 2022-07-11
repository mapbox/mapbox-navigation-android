package com.mapbox.navigation.core

import androidx.annotation.IntDef
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Interface definition for a callback that gets notified whenever routes
 * passed to [MapboxNavigation.setNavigationRoutes] are set or produce an error and ignored.
 */
fun interface RoutesSetCallback {

    /**
     * Invoked whenever the routes passed to [MapboxNavigation.setNavigationRoutes]
     * are successfully set produce an error and are ignored.
     *
     * @param result [RoutesSetResult] object that describes the result of
     *   [MapboxNavigation.setNavigationRoutes] invocation
     */
    fun onRoutesSet(result: RoutesSetResult)
}

/**
 * Represents the result of setting routes. See [RoutesSetCallback.onRoutesSet].
 *
 * @param status status of the [MapboxNavigation.setNavigationRoutes] invocation.
 *   See [RoutesSetStatus] for possible values.
 * @param passedRoutes list of routes that were passed to [MapboxNavigation.setNavigationRoutes].
 * @param primaryRoute status of setting the primary route to the navigator.
 *   If null, no primary route was passed. See [RouteStatus] for more info.
 * @param acceptedAlternatives list of alternative routes that were accepted by the navigator.
 *   See [RouteStatus] for more info.
 * @param ignoredAlternatives list of alternative routes that were ignored by the navigator.
 *   See [RouteStatus] for more info.
 */
class RoutesSetResult internal constructor(
    @Status val status: Int,
    val passedRoutes: List<NavigationRoute>,
    val primaryRoute: RouteStatus?,
    val acceptedAlternatives: List<RouteStatus>,
    val ignoredAlternatives: List<RouteStatus>,
) {

    companion object {
        /**
         * All routes were successfully set to the navigator (both primary route and the alternatives).
         */
        const val SUCCESS = 1

        /**
         * Routes were not set to the navigator because primary route was considered invalid.
         * See details in [RoutesSetResult].
         */
        const val PRIMARY_ROUTE_IGNORED = 2

        /**
         * Primary route was set to the navigator, but some of the alternatives were filtered out.
         * See details in [RoutesSetResult].
         */
        const val ALTERNATIVES_ARE_FILTERED = 3

        /**
         * Retention policy for [RoutesSetStatus].
         */
        @Retention(AnnotationRetention.BINARY)
        @IntDef(
            SUCCESS,
            PRIMARY_ROUTE_IGNORED,
            ALTERNATIVES_ARE_FILTERED,
        )
        annotation class Status
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesSetResult(" +
            "status=$status, " +
            "passedRoutes=$passedRoutes, " +
            "primaryRoute=$primaryRoute, " +
            "acceptedAlternatives=$acceptedAlternatives, " +
            "ignoredAlternatives=$ignoredAlternatives" +
            ")"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesSetResult

        if (status != other.status) return false
        if (passedRoutes != other.passedRoutes) return false
        if (primaryRoute != other.primaryRoute) return false
        if (acceptedAlternatives != other.acceptedAlternatives) return false
        if (ignoredAlternatives != other.ignoredAlternatives) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = status
        result = 31 * result + passedRoutes.hashCode()
        result = 31 * result + primaryRoute.hashCode()
        result = 31 * result + acceptedAlternatives.hashCode()
        result = 31 * result + ignoredAlternatives.hashCode()
        return result
    }
}

/**
 * Represents the result of setting a specific route. See [RoutesSetResult].
 *
 * @param route the original [NavigationRoute] object
 *   that was passed to [MapboxNavigation.setNavigationRoutes].
 * @param wasSet true if the route was accepted by the navigator, false otherwise.
 * @param error a string describing the error if the route was ignored, null if it was accepted.
 */
class RouteStatus internal constructor(
    val route: NavigationRoute,
    val wasSet: Boolean,
    val error: String?,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PrimaryRouteStatus(wasSet=$wasSet, error=$error)"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteStatus

        if (wasSet != other.wasSet) return false
        if (error != other.error) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = wasSet.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
