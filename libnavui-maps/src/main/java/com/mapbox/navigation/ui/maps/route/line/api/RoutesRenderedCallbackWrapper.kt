package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal data class RoutesRenderedCallbackWrapper(
    val map: MapboxMap,
    val callback: RoutesRenderedCallback,
)
