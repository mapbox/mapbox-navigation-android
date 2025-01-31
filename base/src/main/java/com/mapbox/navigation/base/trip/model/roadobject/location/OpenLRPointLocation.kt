package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPosition
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.OPEN_LR_POINT

/**
 * OpenLRPointLocation contains information about the location of the road object represented as
 * OpenLR point on the road graph.
 *
 * @param position position of the object on the graph
 * @param openLRSideOfRoad side of the road where object is situated
 * @param openLROrientation orientation of the object
 */
class OpenLRPointLocation internal constructor(
    val position: EHorizonGraphPosition,
    shape: Geometry,
    @OpenLRSideOfRoad.Type val openLRSideOfRoad: Int,
    @OpenLROrientation.Type val openLROrientation: Int,
) : RoadObjectLocation(OPEN_LR_POINT, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OpenLRPointLocation

        if (position != other.position) return false
        if (openLRSideOfRoad != other.openLRSideOfRoad) return false
        if (openLROrientation != other.openLROrientation) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + openLRSideOfRoad
        result = 31 * result + openLROrientation
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OpenLRPointLocation(" +
            "position=$position, " +
            "openLRSideOfRoad=$openLRSideOfRoad, " +
            "openLROrientation=$openLROrientation" +
            "), ${super.toString()}"
    }
}
