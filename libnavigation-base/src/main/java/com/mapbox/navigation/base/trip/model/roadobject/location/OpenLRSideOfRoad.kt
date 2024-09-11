package com.mapbox.navigation.base.trip.model.roadobject.location

import androidx.annotation.IntDef

/**
 * OpenLRSideOfRoad describes the relationship between the road object and the road.
 * The road object can be on the right side of the road, on the left side of the road, on both
 * sides of the road or directly on the road.
 */
object OpenLRSideOfRoad {
    /**
     * Type of the [ON_ROAD_OR_UNKNOWN].
     */
    const val ON_ROAD_OR_UNKNOWN = 0

    /**
     * Type of the [RIGHT].
     */
    const val RIGHT = 1

    /**
     * Type of the [LEFT].
     */
    const val LEFT = 2

    /**
     * Type of the [BOTH].
     */
    const val BOTH = 3

    /**
     * Retention policy for the OpenLRSideOfRoad
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        ON_ROAD_OR_UNKNOWN,
        RIGHT,
        LEFT,
        BOTH,
    )
    annotation class Type
}
