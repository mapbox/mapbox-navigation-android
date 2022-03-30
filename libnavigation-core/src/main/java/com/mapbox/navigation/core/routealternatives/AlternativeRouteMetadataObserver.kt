package com.mapbox.navigation.core.routealternatives

fun interface AlternativeRouteMetadataObserver {
    fun onMetadataUpdated(metadata: List<AlternativeRouteMetadata>)
}
