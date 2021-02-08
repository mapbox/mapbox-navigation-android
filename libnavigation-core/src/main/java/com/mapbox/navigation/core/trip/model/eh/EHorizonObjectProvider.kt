package com.mapbox.navigation.core.trip.model.eh

/**
 * RoadObject provider
 */
object EHorizonObjectProvider {
    /**
     * Road object was provided via Mapbox services
     */
    const val MAPBOX = "MAPBOX"

    /**
     * Road object was added by user
     * (via `mapboxNavigation.getEHorizonObjectsStore.addCustomRoadObject()`)
     */
    const val CUSTOM = "CUSTOM"
}
