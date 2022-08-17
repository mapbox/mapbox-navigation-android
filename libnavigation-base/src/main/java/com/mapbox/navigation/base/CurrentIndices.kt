package com.mapbox.navigation.base

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Class holding information about a snapshot of current indices.
 * All the indices are consistent (taken from the same RouteProgress instance).
 *
 * @param legIndex index of a leg the user is currently on.
 * @param routeGeometryIndex route-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 * @param legGeometryIndex leg-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 */
class CurrentIndices internal constructor(
    val legIndex: Int,
    val routeGeometryIndex: Int,
    val legGeometryIndex: Int?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrentIndices

        if (legIndex != other.legIndex) return false
        if (routeGeometryIndex != other.routeGeometryIndex) return false
        if (legGeometryIndex != other.legGeometryIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = legIndex
        result = 31 * result + routeGeometryIndex
        result = 31 * result + (legGeometryIndex ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CurrentIndices(" +
            "legIndex=$legIndex, " +
            "routeGeometryIndex=$routeGeometryIndex, " +
            "legGeometryIndex=$legGeometryIndex" +
            ")"
    }
}
