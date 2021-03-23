package com.mapbox.navigation.core.trip.model.roadobject

import androidx.annotation.IntDef
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject

/**
 * Holds available [RoadObject] types.
 *
 * Available values are:
 * - [RoadObjectType.TUNNEL_ENTRANCE]
 * - [RoadObjectType.TUNNEL_EXIT]
 * - [RoadObjectType.COUNTRY_BORDER_CROSSING]
 * - [RoadObjectType.TOLL_COLLECTION]
 * - [RoadObjectType.REST_STOP]
 * - [RoadObjectType.RESTRICTED_AREA_ENTRANCE]
 * - [RoadObjectType.RESTRICTED_AREA_EXIT]
 * - [RoadObjectType.BRIDGE_ENTRANCE]
 * - [RoadObjectType.BRIDGE_EXIT]
 * - [RoadObjectType.INCIDENT]
 * - [RoadObjectType.CUSTOM]
 */
object RoadObjectType {

    /**
     * Type of the [TUNNEL_ENTRANCE].
     */
    const val TUNNEL_ENTRANCE = 0

    /**
     * Type of the [TUNNEL_EXIT].
     */
    const val TUNNEL_EXIT = 1

    /**
     * Type of the [COUNTRY_BORDER_CROSSING].
     */
    const val COUNTRY_BORDER_CROSSING = 2

    /**
     * Type of the [TOLL_COLLECTION].
     */
    const val TOLL_COLLECTION = 3

    /**
     * Type of the [REST_STOP].
     */
    const val REST_STOP = 4

    /**
     * Type of the [RESTRICTED_AREA_ENTRANCE].
     */
    const val RESTRICTED_AREA_ENTRANCE = 5

    /**
     * Type of the [RESTRICTED_AREA_EXIT].
     */
    const val RESTRICTED_AREA_EXIT = 6

    /**
     * Type of the [BRIDGE_ENTRANCE].
     */
    const val BRIDGE_ENTRANCE = 7

    /**
     * Type of the [BRIDGE_EXIT].
     */
    const val BRIDGE_EXIT = 8

    /**
     * Type of the [INCIDENT].
     */
    const val INCIDENT = 9

    /**
     * Type of the [CUSTOM].
     */
    const val CUSTOM = 10

    /**
     * Retention policy for the EHorizonObjectType
     */
    @Retention
    @IntDef(
        TUNNEL_ENTRANCE,
        TUNNEL_EXIT,
        COUNTRY_BORDER_CROSSING,
        TOLL_COLLECTION,
        REST_STOP,
        RESTRICTED_AREA_ENTRANCE,
        RESTRICTED_AREA_EXIT,
        BRIDGE_ENTRANCE,
        BRIDGE_EXIT,
        INCIDENT,
        CUSTOM,
    )
    annotation class Type
}
