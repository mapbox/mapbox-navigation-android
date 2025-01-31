package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPath
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.POLYLINE

/**
 * PolylineLocation contains information about the location of the road object represented as
 * polyline on the road graph.
 *
 * @param path path on the graph
 */
class PolylineLocation internal constructor(
    val path: EHorizonGraphPath,
    shape: Geometry,
) : RoadObjectLocation(POLYLINE, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PolylineLocation

        if (path != other.path) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PolylineLocation(" +
            "path=$path" +
            "), ${super.toString()}"
    }
}
