package com.mapbox.navigation.core.trip.session

import com.mapbox.navigator.RouteAlternative

internal data class NativeSetRouteResult(
    val nativeAlternatives: List<RouteAlternative>? = null
)
