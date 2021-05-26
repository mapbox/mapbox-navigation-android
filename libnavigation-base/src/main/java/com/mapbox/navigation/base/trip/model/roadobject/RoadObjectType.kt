package com.mapbox.navigation.base.trip.model.roadobject

import androidx.annotation.IntDef

/**
 * Holds available [RoadObject] types.
 *
 * Available values are:
 * - [RoadObjectType.TUNNEL]
 * - [RoadObjectType.COUNTRY_BORDER_CROSSING]
 * - [RoadObjectType.TOLL_COLLECTION]
 * - [RoadObjectType.REST_STOP]
 * - [RoadObjectType.RESTRICTED_AREA]
 * - [RoadObjectType.BRIDGE]
 * - [RoadObjectType.INCIDENT]
 * - [RoadObjectType.CUSTOM]
 */
object RoadObjectType {

    /**
     * Type of the [TUNNEL].
     */
    const val TUNNEL = 0

    /**
     * Type of the [COUNTRY_BORDER_CROSSING].
     */
    const val COUNTRY_BORDER_CROSSING = 1

    /**
     * Type of the [TOLL_COLLECTION].
     */
    const val TOLL_COLLECTION = 2

    /**
     * Type of the [REST_STOP].
     */
    const val REST_STOP = 3

    /**
     * Type of the [RESTRICTED_AREA].
     */
    const val RESTRICTED_AREA = 4

    /**
     * Type of the [BRIDGE].
     */
    const val BRIDGE = 5

    /**
     * Type of the [INCIDENT].
     */
    const val INCIDENT = 6

    /**
     * Type of the [CUSTOM].
     */
    const val CUSTOM = 7

    /**
     * Retention policy for the RoadObjectType
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        TUNNEL,
        COUNTRY_BORDER_CROSSING,
        TOLL_COLLECTION,
        REST_STOP,
        RESTRICTED_AREA,
        BRIDGE,
        INCIDENT,
        CUSTOM,
    )
    annotation class Type
}
