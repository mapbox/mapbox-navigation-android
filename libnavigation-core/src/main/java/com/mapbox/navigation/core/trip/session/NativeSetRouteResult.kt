package com.mapbox.navigation.core.trip.session

import com.mapbox.navigator.RouteAlternative

internal sealed class NativeSetRouteResult

internal class NativeSetRouteValue(
    val nativeAlternatives: List<RouteAlternative>
) : NativeSetRouteResult()

internal class NativeSetRouteError(
    val error: String? = null,
) : NativeSetRouteResult()
