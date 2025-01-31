package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Class holding information about dynamic data used for refresh requests.
 *
 * @param legIndex index of a leg the user is currently on.
 * @param routeGeometryIndex route-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 * @param legGeometryIndex leg-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 * @param experimentalProperties map containing dynamic data.
 */
class RouteRefreshRequestData(
    val legIndex: Int,
    val routeGeometryIndex: Int,
    val legGeometryIndex: Int?,
    val experimentalProperties: Map<String, String>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshRequestData

        if (legIndex != other.legIndex) return false
        if (routeGeometryIndex != other.routeGeometryIndex) return false
        if (legGeometryIndex != other.legGeometryIndex) return false
        if (experimentalProperties != other.experimentalProperties) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = legIndex
        result = 31 * result + routeGeometryIndex
        result = 31 * result + (legGeometryIndex ?: 0)
        result = 31 * result + experimentalProperties.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshRequestData(" +
            "legIndex=$legIndex, " +
            "routeGeometryIndex=$routeGeometryIndex, " +
            "legGeometryIndex=$legGeometryIndex, " +
            "experimentalProperties=$experimentalProperties" +
            ")"
    }
}
