package com.mapbox.navigation.base.trip.model.roadobject

import androidx.annotation.StringDef

/**
 * RoadObject provider
 */
object RoadObjectProvider {
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
     * Retention policy for the RoadObjectProvider
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        MAPBOX,
        CUSTOM,
    )
    annotation class Type
}
