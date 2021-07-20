package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.internal.extensions.notEquals
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * PolygonDistanceInfo contains information about distance to the road object represented as
 * polygon.
 *
 * @param distanceToNearestEntry distance to the nearest entry
 * @param distanceToNearestExit distance to the nearest exit
 * @param inside true if inside the object
 */
class PolygonDistanceInfo internal constructor(
    roadObjectId: String,
    @RoadObjectType.Type roadObjectType: Int,
    val distanceToNearestEntry: Double,
    val distanceToNearestExit: Double,
    val inside: Boolean,
) : RoadObjectDistanceInfo(roadObjectId, roadObjectType, RoadObjectDistanceInfoType.POLYGON) {

    /**
     * distance to start of the object
     */
    override val distanceToStart: Double = distanceToNearestEntry

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PolygonDistanceInfo

        if (distanceToNearestEntry.notEquals(other.distanceToNearestEntry)) return false
        if (distanceToNearestExit.notEquals(other.distanceToNearestExit)) return false
        if (inside != other.inside) return false
        if (distanceToStart.notEquals(other.distanceToStart)) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + distanceToNearestEntry.hashCode()
        result = 31 * result + distanceToNearestExit.hashCode()
        result = 31 * result + inside.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PolygonDistanceInfo(" +
            "distanceToNearestEntry=$distanceToNearestEntry, " +
            "distanceToNearestExit=$distanceToNearestExit, " +
            "inside=$inside, " +
            "distanceToStart=$distanceToStart" +
            "), ${super.toString()}"
    }
}
