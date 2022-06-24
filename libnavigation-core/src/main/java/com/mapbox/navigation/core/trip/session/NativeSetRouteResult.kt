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
) : NativeSetRouteResult()

/**
 * Represents a failure of setting routes to native navigator.
 * @param error Reason why the routes were not set.
 */
internal class NativeSetRouteError(
    val error: String? = null,
) : NativeSetRouteResult()
