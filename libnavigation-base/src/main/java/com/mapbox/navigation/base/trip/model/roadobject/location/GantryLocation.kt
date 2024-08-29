package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.GANTRY

/**
 * GantryLocation contains information about the location of the road object represented as gantry
 * on the road graph.
 *
 * @param positions intersection points of the gantry with the road graph
 */
class GantryLocation internal constructor(
    val positions: List<RoadObjectPosition>,
    shape: Geometry,
) : RoadObjectLocation(GANTRY, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GantryLocation

        if (positions != other.positions) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + positions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "GantryLocation(" +
            "positions=$positions" +
            "), ${super.toString()}"
    }
}
