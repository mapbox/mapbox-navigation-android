package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * RoadObjectDistanceInfo contains information about distance to the road object of a concrete
 * flavor/shape (gantry, polygon, line, point etc.)
 *
 * Available types are:
 * - [RoadObjectDistanceInfoType.GANTRY]
 * - [RoadObjectDistanceInfoType.LINE]
 * - [RoadObjectDistanceInfoType.POINT]
 * - [RoadObjectDistanceInfoType.POLYGON]
 * - [RoadObjectDistanceInfoType.SUB_GRAPH]
 *
 * @param roadObjectId id of the road object
 * @param roadObjectType type of the road object
 * @param distanceInfoType type of the distance info object
 */
abstract class RoadObjectDistanceInfo internal constructor(
    val roadObjectId: String,
    @RoadObjectType.Type val roadObjectType: Int,
    @RoadObjectDistanceInfoType.Type val distanceInfoType: Int,
) {

    /**
     * Distance to start of the object from current location or null if couldn't be determined.
     */
    abstract val distanceToStart: Double?

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectDistanceInfo

        if (roadObjectId != other.roadObjectId) return false
        if (roadObjectType != other.roadObjectType) return false
        if (distanceInfoType != other.distanceInfoType) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + roadObjectType
        result = 31 * result + distanceInfoType
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObjectDistanceInfo(" +
            "roadObjectId='$roadObjectId', " +
            "roadObjectType=$roadObjectType, " +
            "distanceInfoType=$distanceInfoType, " +
            "distanceToStart=$distanceToStart" +
            ")"
    }
}
