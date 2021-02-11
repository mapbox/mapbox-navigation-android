package com.mapbox.navigation.core.trip.model.eh

import androidx.annotation.StringDef

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

    /**
     * Retention policy for the EHorizonObjectProvider
     */
    @Retention
    @StringDef(MAPBOX, CUSTOM)
    annotation class Type
}
