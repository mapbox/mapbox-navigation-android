package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Abstract class that serves as a base for all route alerts.
 *
 * Available alert types are:
 * - [TunnelEntranceAlert]
 * - [BorderCrossingAlert]
 * - [TollCollectionAlert]
 * - [RestStopAlert]
 * - [RestrictedAreaAlert]
 *
 * @param type constant describing the alert type, see [RouteAlertType].
 * @param metadata type-safe metadata of each of the event.
 * @param coordinate location of the alert or its start point in case it has a length
 * @param distance distance to this alert since the start of the route
 * @param alertGeometry optional geometry details of the alert if it has a length
 */
abstract class RouteAlert<Metadata>(
    val type: Int,
    val metadata: Metadata,
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

        other as RouteAlert<*>

        if (type != other.type) return false
        if (metadata != other.metadata) return false
        if (coordinate != other.coordinate) return false
        if (distance != other.distance) return false
        if (alertGeometry != other.alertGeometry) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + coordinate.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + (alertGeometry?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlert(type=$type, " +
            "metadata=$metadata, " +
            "coordinate=$coordinate, " +
            "distance=$distance, " +
            "alertGeometry=$alertGeometry)"
    }
}
