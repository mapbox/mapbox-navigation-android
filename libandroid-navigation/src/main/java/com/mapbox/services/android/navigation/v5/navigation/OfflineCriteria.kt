package com.mapbox.services.android.navigation.v5.navigation

import androidx.annotation.StringDef

object OfflineCriteria {

    /**
     * Bicycle type for road bike.
     */
    const val ROAD = "Road"

    /**
     * Bicycle type for hybrid bike.
     */
    const val HYBRID = "Hybrid"

    /**
     * Bicycle type for city bike.
     */
    const val CITY = "City"

    /**
     * Bicycle type for cross bike.
     */
    const val CROSS = "Cross"

    /**
     * Bicycle type for mountain bike.
     */
    const val MOUNTAIN = "Mountain"

    /**
     * Break waypoint type.
     */
    const val BREAK = "break"

    /**
     * Through waypoint type.
     */
    const val THROUGH = "through"

    /**
     * Retention policy for the bicycle type parameter in the Directions API.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ROAD, HYBRID, CITY, CROSS, MOUNTAIN)
    annotation class BicycleType

    /**
     * Retention policy for the waypoint type parameter in the Directions API.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(BREAK, THROUGH)
    annotation class WaypointType
}
