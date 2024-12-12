package com.mapbox.navigation.ui.maps.internal.extensions

import androidx.annotation.RestrictTo
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxRouteLineView.clearFinallyInternal(mapboxMap: MapboxMap) {
    clearFinally(mapboxMap)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxRouteLineApi.clearFinallyInternal() {
    clearFinally()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxRouteArrowView.clearFinallyInternal(mapboxMap: MapboxMap) {
    clearFinally(mapboxMap)
}
