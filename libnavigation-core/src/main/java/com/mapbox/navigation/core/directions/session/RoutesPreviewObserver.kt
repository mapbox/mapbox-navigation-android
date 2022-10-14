package com.mapbox.navigation.core.directions.session

fun interface RoutesPreviewObserver {
    // What happens during active navigation
    fun onRoutesPreviewChanged(routePreview: RoutePreview)
}
