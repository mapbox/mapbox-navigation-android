package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * GantryDistanceInfo contains information about distance to the road object represented as gantry.
 *
 */
class GantryDistanceInfo internal constructor(
    roadObjectId: String,
    @RoadObjectType.Type roadObjectType: Int,
    distance: Double,
) : RoadObjectDistanceInfo(roadObjectId, roadObjectType, RoadObjectDistanceInfoType.GANTRY) {

    /**
     * distance to start of the object
     */
    override val distanceToStart: Double = distance

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GantryDistanceInfo

        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "GantryDistanceInfo(" +
            "distanceToStart=$distanceToStart" +
            "), ${super.toString()}"
    }
}
