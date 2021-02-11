package com.mapbox.navigation.core.trip.model.eh

import androidx.annotation.StringDef

/**
 * RoadObject type
 */
object EHorizonObjectType {

    /**
     * Road object represents some road incident
     */
    const val INCIDENT = "INCIDENT"

    /**
     * Road object was added by user
     * (via `mapboxNavigation.getEHorizonObjectsStore.addCustomRoadObject()`)
     */
    const val CUSTOM = "CUSTOM"

    /**
     * Retention policy for the EHorizonObjectType
     */
    @Retention
    @StringDef(INCIDENT, CUSTOM)
    annotation class Type
}
