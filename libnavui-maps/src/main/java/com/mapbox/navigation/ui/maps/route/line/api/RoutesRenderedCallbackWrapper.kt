package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.MapboxMap

internal data class RoutesRenderedCallbackWrapper(
    val map: MapboxMap,
    val callback: DelayedRoutesRenderedCallback,
)
