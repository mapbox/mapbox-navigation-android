package com.mapbox.navigation.base.trip.model.roadobject

import androidx.annotation.IntDef

/**
 * Holds available [RoadObject] types.
 *
 * Available values are:
 * - [RoadObjectType.TUNNEL_ENTRANCE]
 * - [RoadObjectType.TUNNEL_EXIT]
 * - [RoadObjectType.TUNNEL]
 * - [RoadObjectType.COUNTRY_BORDER_CROSSING]
 * - [RoadObjectType.TOLL_COLLECTION]
 * - [RoadObjectType.REST_STOP]
 * - [RoadObjectType.RESTRICTED_AREA_ENTRANCE]
 * - [RoadObjectType.RESTRICTED_AREA_EXIT]
 * - [RoadObjectType.RESTRICTED_AREA]
 * - [RoadObjectType.BRIDGE_ENTRANCE]
 * - [RoadObjectType.BRIDGE_EXIT]
 * - [RoadObjectType.BRIDGE]
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
     * Type of the [TUNNEL].
     */
    const val TUNNEL = 2

    /**
     * Type of the [COUNTRY_BORDER_CROSSING].
     */
    const val COUNTRY_BORDER_CROSSING = 3

    /**
     * Type of the [TOLL_COLLECTION].
     */
    const val TOLL_COLLECTION = 4

    /**
     * Type of the [REST_STOP].
     */
    const val REST_STOP = 5

    /**
     * Type of the [RESTRICTED_AREA_ENTRANCE].
     */
    const val RESTRICTED_AREA_ENTRANCE = 6

    /**
     * Type of the [RESTRICTED_AREA_EXIT].
     */
    const val RESTRICTED_AREA_EXIT = 7

    /**
     * Type of the [RESTRICTED_AREA].
     */
    const val RESTRICTED_AREA = 8

    /**
     * Type of the [BRIDGE_ENTRANCE].
     */
    const val BRIDGE_ENTRANCE = 9

    /**
     * Type of the [BRIDGE_EXIT].
     */
    const val BRIDGE_EXIT = 10

    /**
     * Type of the [BRIDGE].
     */
    const val BRIDGE = 11

    /**
     * Type of the [INCIDENT].
     */
    const val INCIDENT = 12

    /**
     * Type of the [CUSTOM].
     */
    const val CUSTOM = 13

    /**
     * Retention policy for the EHorizonObjectType
     */
    @Retention
    @IntDef(
        TUNNEL_ENTRANCE,
        TUNNEL_EXIT,
        TUNNEL,
        COUNTRY_BORDER_CROSSING,
        TOLL_COLLECTION,
        REST_STOP,
        RESTRICTED_AREA_ENTRANCE,
        RESTRICTED_AREA_EXIT,
        RESTRICTED_AREA,
        BRIDGE_ENTRANCE,
        BRIDGE_EXIT,
        BRIDGE,
        INCIDENT,
        CUSTOM,
    )
    annotation class Type
}
