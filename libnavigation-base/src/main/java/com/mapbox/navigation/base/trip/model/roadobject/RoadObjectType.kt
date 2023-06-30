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
 * - [RoadObjectType.RAILWAY_CROSSING]
 * - [RoadObjectType.IC]
 * - [RoadObjectType.JCT]
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
     * Type of the [RAILWAY_CROSSING].
     */
    const val RAILWAY_CROSSING = 8

    /**
     * Type of the [IC] - corresponds to interchange.
     */
    const val IC = 9

    /**
     * Type of the [JCT] - corresponds to junction.
     */
    const val JCT = 10

    /**
     * Not finished yet, see https://mapbox.atlassian.net/browse/NAVAND-1311
     */
    internal const val NOTIFICATION = 11

    /**
     * Type of the [MERGING_AREA] - corresponds to Merging Area.
     */
    internal const val MERGING_AREA = 12

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
        RAILWAY_CROSSING,
        IC,
        JCT,
    )
    annotation class Type
}
