package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.POINT

/**
 * PointLocation contains information about the location of the road object represented as point
 * on the road graph.
 *
 * @param position position of the object on the edge
 */
class PointLocation internal constructor(
    val position: RoadObjectPosition,
    shape: Geometry,
) : RoadObjectLocation(POINT, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PointLocation

        if (position != other.position) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PointLocation(" +
            "position=$position" +
            "), ${super.toString()}"
    }
}
