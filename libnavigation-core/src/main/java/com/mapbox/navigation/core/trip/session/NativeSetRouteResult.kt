package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteAlternative

/**
 * Represents what native navigator returned after `setRoute` invocation.
 * Can be either [NativeSetRouteValue] for success or [NativeSetRouteError] for failure.
 */
internal sealed class NativeSetRouteResult

/**
 * Represents successful setting of routes to native navigator.
 * @param nativeAlternatives Set routes.
 */
internal class NativeSetRouteValue(
    val routes: List<NavigationRoute>,
    val nativeAlternatives: List<RouteAlternative>,
) : NativeSetRouteResult() {

    override fun toString(): String {
        return "NativeSetRouteValue(routes=$routes, nativeAlternatives=$nativeAlternatives)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeSetRouteValue

        if (routes != other.routes) return false
        if (nativeAlternatives != other.nativeAlternatives) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routes.hashCode()
        result = 31 * result + nativeAlternatives.hashCode()
        return result
    }
}

/**
 * Represents a failure of setting routes to native navigator.
 * @param error Reason why the routes were not set.
 */
internal class NativeSetRouteError(
    val error: String,
) : NativeSetRouteResult() {

    override fun toString(): String {
        return "NativeSetRouteError(error='$error')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeSetRouteError

        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        return error.hashCode()
    }
}
