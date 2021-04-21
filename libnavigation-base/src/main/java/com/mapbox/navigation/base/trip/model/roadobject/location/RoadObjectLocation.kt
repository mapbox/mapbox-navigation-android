package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry

/**
 * RoadObjectLocation contains information about the location of the road object of a concrete
 * flavor/shape (gantry, polygon, line, point etc.) on the road graph.
 *
 * @param locationType type of the road object location
 * @param shape geometry of the road object
 *
 * Available values are:
 * - [RoadObjectLocationType.GANTRY]
 * - [RoadObjectLocationType.OPEN_LR_LINE]
 * - [RoadObjectLocationType.OPEN_LR_POINT]
 * - [RoadObjectLocationType.POINT]
 * - [RoadObjectLocationType.POLYGON]
 * - [RoadObjectLocationType.POLYLINE]
 * - [RoadObjectLocationType.POLYLINE]
 * - [RoadObjectLocationType.ROUTE_ALERT]
 */
abstract class RoadObjectLocation internal constructor(
    @RoadObjectLocationType.Type val locationType: Int,
    val shape: Geometry,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectLocation

        if (locationType != other.locationType) return false
        if (shape != other.shape) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = locationType
        result = 31 * result + shape.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObjectLocation(" +
            "locationType=$locationType, " +
            "shape=$shape" +
            ")"
    }
}
