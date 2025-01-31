package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPath
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.OPEN_LR_LINE

/**
 * OpenLRLineLocation contains information about the location of the road object represented as
 * OpenLR line on the road graph.
 *
 * @param graphPath path of the object location on the graph.
 */
class OpenLRLineLocation internal constructor(
    val graphPath: EHorizonGraphPath,
    shape: Geometry,
) : RoadObjectLocation(OPEN_LR_LINE, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OpenLRLineLocation

        if (graphPath != other.graphPath) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + graphPath.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OpenLRLineLocation(" +
            "graphPath=$graphPath" +
            "), ${super.toString()}"
    }
}
