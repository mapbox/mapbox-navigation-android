package com.mapbox.navigation.core.trip.session

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
    val nativeAlternatives: List<RouteAlternative>
) : NativeSetRouteResult() {

    override fun toString(): String {
        return "NativeSetRouteValue(nativeAlternatives=$nativeAlternatives)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeSetRouteValue

        if (nativeAlternatives != other.nativeAlternatives) return false

        return true
    }

    override fun hashCode(): Int {
        return nativeAlternatives.hashCode()
    }
}

/**
 * Represents a failure of setting routes to native navigator.
 * @param error Reason why the routes were not set.
 */
internal class NativeSetRouteError(
    val error: String
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
