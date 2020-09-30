package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Abstract class that serves as a base for all route alerts.
 *
 * Available alert types are:
 * - [TunnelEntranceAlert]
 * - [CountryBorderCrossingAlert]
 * - [TollCollectionAlert]
 * - [RestStopAlert]
 * - [RestrictedAreaAlert]
 *
 * @param alertType constant describing the alert type, see [RouteAlertType].
 * @param coordinate location of the alert or its start point in case it has a length
 * @param distance distance to this alert since the start of the route
 * @param alertGeometry optional geometry details of the alert if it has a length
 */
abstract class RouteAlert(
    val alertType: Int,
    val coordinate: Point,
    val distance: Double,
    val alertGeometry: RouteAlertGeometry?
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlert

        if (alertType != other.alertType) return false
        if (coordinate != other.coordinate) return false
        if (distance != other.distance) return false
        if (alertGeometry != other.alertGeometry) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = alertType.hashCode()
        result = 31 * result + coordinate.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + (alertGeometry?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlert(type=$alertType, " +
            "coordinate=$coordinate, " +
            "distance=$distance, " +
            "alertGeometry=$alertGeometry)"
    }
}
