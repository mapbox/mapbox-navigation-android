package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.POLYGON

/**
 * PolygonLocation contains information about the location of the road object represented as polygon
 * on the road graph.
 *
 * @param entries list of entries locations
 * @param exits list of exits locations
 */
class PolygonLocation internal constructor(
    val entries: List<RoadObjectPosition>,
    val exits: List<RoadObjectPosition>,
    shape: Geometry,
) : RoadObjectLocation(POLYGON, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PolygonLocation

        if (entries != other.entries) return false
        if (exits != other.exits) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + entries.hashCode()
        result = 31 * result + exits.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PolygonLocation(" +
            "entries=$entries, " +
            "exits=$exits" +
            "), ${super.toString()}"
    }
}
