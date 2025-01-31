package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import androidx.annotation.IntDef

/**
 * Holds available [RoadObjectDistanceInfo] types.
 *
 * Available values are:
 * - [RoadObjectDistanceInfoType.GANTRY]
 * - [RoadObjectDistanceInfoType.LINE]
 * - [RoadObjectDistanceInfoType.POINT]
 * - [RoadObjectDistanceInfoType.POLYGON]
 * - [RoadObjectDistanceInfoType.SUB_GRAPH]
 */
object RoadObjectDistanceInfoType {

    /**
     * Type of the [GANTRY].
     */
    const val GANTRY = 0

    /**
     * Type of the [LINE].
     */
    const val LINE = 1

    /**
     * Type of the [POINT].
     */
    const val POINT = 2

    /**
     * Type of the [POLYGON].
     */
    const val POLYGON = 3

    /**
     * Type of the [SUB_GRAPH].
     */
    const val SUB_GRAPH = 4

    /**
     * Retention policy for the RoadObjectDistanceInfoType
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        GANTRY,
        LINE,
        POINT,
        POLYGON,
        SUB_GRAPH,
    )
    annotation class Type
}
