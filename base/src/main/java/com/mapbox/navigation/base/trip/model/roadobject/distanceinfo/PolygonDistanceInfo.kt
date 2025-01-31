package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * PolygonDistanceInfo contains information about distance to the road object represented as
 * polygon.
 *
 * @param entrances distances to particular entrances, sorted by probability.
 * Might be empty in the case when the polygon is very large and no entry could be detected using most likely path.
 * @param exits distances to particular exits, sorted by probability.
 * Might be empty in the case when the polygon is very large and no exit could be detected using most probable path.
 * @param inside true if inside the object
 */
class PolygonDistanceInfo internal constructor(
    roadObjectId: String,
    @RoadObjectType.Type roadObjectType: Int,
    val entrances: List<Gate>,
    val exits: List<Gate>,
    val inside: Boolean,
) : RoadObjectDistanceInfo(roadObjectId, roadObjectType, RoadObjectDistanceInfoType.POLYGON) {

    /**
     * distance to start of the object or null if couldn't be determined.
     */
    override val distanceToStart: Double? = entrances.firstOrNull()?.distance

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PolygonDistanceInfo

        if (entrances != other.entrances) return false
        if (exits != other.exits) return false
        if (inside != other.inside) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + entrances.hashCode()
        result = 31 * result + exits.hashCode()
        result = 31 * result + inside.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PolygonDistanceInfo(" +
            "entrances=$entrances, " +
            "exits=$exits, " +
            "inside=$inside, " +
            "distanceToStart=$distanceToStart" +
            "), ${super.toString()}"
    }
}
