package com.mapbox.navigation.core.directions.session

fun interface RoutesPreviewObserver {
    fun onRoutesPreviewChanged(routesPreview: RoutesPreview)
}
