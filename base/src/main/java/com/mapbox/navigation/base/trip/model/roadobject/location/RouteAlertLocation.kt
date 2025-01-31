package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType.ROUTE_ALERT

/**
 * RouteAlertLocation contains information about the location of the route alert.
 * It will be produced only for objects that are on the current route that we are actively
 * navigating on.
 *
 */
class RouteAlertLocation internal constructor(
    shape: Geometry,
) : RoadObjectLocation(ROUTE_ALERT, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return super.toString()
    }
}
