package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

/**
 * Describes the geometry of a [RouteAlert] that has a length.
 *
 * Use [LineString.fromPolyline] with [DirectionsRoute.geometry] and the precision argument
 * that was used when requesting the route ([RouteOptions.Builder.geometries]) to decode the route.
 * When decoded, you can find the geometry of the alert using
 * the `startGeometryIndex` and `endGeometryIndex` values as arguments for [LineString.coordinates].
 * This sublist of coordinates can be used to
 * create and draw the geometry with [LineString.fromLngLats].
 *
 * Also see [RouteAlertGeometry.toLineString] extension which automates above.
 *
 * @param length length of the alert.
 * @param startCoordinate point where the alert starts.
 * @param startGeometryIndex index of a point in the route geometry where the alert starts.
 * @param endCoordinate point where the alert ends.
 * @param endGeometryIndex index of a point in the route geometry where the alert ends.
 */
class RouteAlertGeometry private constructor(
    val length: Double,
    val startCoordinate: Point,
    val startGeometryIndex: Int,
    val endCoordinate: Point,
    val endGeometryIndex: Int
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder() = Builder(
        length,
        startCoordinate,
        startGeometryIndex,
        endCoordinate,
        endGeometryIndex
    )

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlertGeometry

        if (length != other.length) return false
        if (startCoordinate != other.startCoordinate) return false
        if (startGeometryIndex != other.startGeometryIndex) return false
        if (endCoordinate != other.endCoordinate) return false
        if (endGeometryIndex != other.endGeometryIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + startCoordinate.hashCode()
        result = 31 * result + startGeometryIndex
        result = 31 * result + endCoordinate.hashCode()
        result = 31 * result + endGeometryIndex
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlertGeometry(" +
            "length=$length," +
            "startCoordinate=$startCoordinate," +
            "startGeometryIndex=$startGeometryIndex," +
            "endCoordinate=$endCoordinate," +
            "endGeometryIndex=$endGeometryIndex"
    }

    /**
     * Use to create a new instance.
     *
     * @see RouteAlertGeometry
     */
    class Builder(
        private val length: Double,
        private val startCoordinate: Point,
        private val startGeometryIndex: Int,
        private val endCoordinate: Point,
        private val endGeometryIndex: Int
    ) {

        /**
         * Build the object instance.
         */
        fun build() = RouteAlertGeometry(
            length,
            startCoordinate,
            startGeometryIndex,
            endCoordinate,
            endGeometryIndex
        )
    }
}
