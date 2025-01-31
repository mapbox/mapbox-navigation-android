package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * LineDistanceInfo contains information about distance to the road object represented as line.
 *
 * @param distanceToEntry distance to the entry of the object
 * @param distanceToExit distance to the most likely exit
 * @param distanceToEnd distance to the end of the most distant exit
 * @param entryFromStart confirms if we enter a line object from start or from some other point along
 * @param length length of the object
 */
class LineDistanceInfo internal constructor(
    roadObjectId: String,
    @RoadObjectType.Type roadObjectType: Int,
    val distanceToEntry: Double,
    val distanceToExit: Double,
    val distanceToEnd: Double,
    val entryFromStart: Boolean,
    val length: Double,
) : RoadObjectDistanceInfo(roadObjectId, roadObjectType, RoadObjectDistanceInfoType.LINE) {

    /**
     * distance to start of the object
     */
    override val distanceToStart: Double = distanceToEntry

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as LineDistanceInfo

        if (distanceToEntry != other.distanceToEntry) return false
        if (distanceToExit != other.distanceToExit) return false
        if (distanceToEnd != other.distanceToEnd) return false
        if (entryFromStart != other.entryFromStart) return false
        if (length != other.length) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + distanceToEntry.hashCode()
        result = 31 * result + distanceToExit.hashCode()
        result = 31 * result + distanceToEnd.hashCode()
        result = 31 * result + entryFromStart.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LineDistanceInfo(" +
            "distanceToEntry=$distanceToEntry, " +
            "distanceToExit=$distanceToExit, " +
            "distanceToEnd=$distanceToEnd, " +
            "entryFromStart=$entryFromStart, " +
            "length=$length, " +
            "distanceToStart=$distanceToStart" +
            "), ${super.toString()}"
    }
}
