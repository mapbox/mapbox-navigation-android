package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPosition

/**
 * RoadObjectPosition contains information about position of the point on the graph and
 * it's geo-position.
 *
 * @param eHorizonGraphPosition position on the graph
 * @param coordinate position of the object
 */
class RoadObjectPosition internal constructor(
    val eHorizonGraphPosition: EHorizonGraphPosition,
    val coordinate: Point,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectPosition

        if (eHorizonGraphPosition != other.eHorizonGraphPosition) return false
        if (coordinate != other.coordinate) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = eHorizonGraphPosition.hashCode()
        result = 31 * result + coordinate.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObjectPosition(" +
            "eHorizonGraphPosition=$eHorizonGraphPosition, " +
            "coordinate=$coordinate" +
            ")"
    }
}
