package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString

/**
 * Describes the geometry of a [RoadObject] that has a length.
 *
 * Use [LineString.fromPolyline] with [DirectionsRoute.geometry] and the precision argument
 * that was used when requesting the route ([RouteOptions.Builder.geometries]) to decode the route.
 * When decoded, you can find the geometry of the object using
 * the `startGeometryIndex` and `endGeometryIndex` values as arguments for [LineString.coordinates].
 * This sublist of coordinates can be used to
 * create and draw the geometry with [LineString.fromLngLats].
 *
 * Also see [RoadObjectGeometry.toLineString] extension which automates above.
 *
 * @param length length of the object, null if the object is point-like.
 * @param shape shape of the object.
 * @param startGeometryIndex index of a point in the route geometry where the object starts
 * Will be null for road objects returned by Electronic Horizon.
 * @param endGeometryIndex index of a point in the route geometry where the object ends.
 * Will be null for road objects returned by Electronic Horizon.
 */
class RoadObjectGeometry private constructor(
    val length: Double?,
    val shape: Geometry,
    val startGeometryIndex: Int?,
    val endGeometryIndex: Int?,
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder() = Builder(
        length,
        shape,
        startGeometryIndex,
        endGeometryIndex
    )

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectGeometry

        if (length != other.length) return false
        if (shape != other.shape) return false
        if (startGeometryIndex != other.startGeometryIndex) return false
        if (endGeometryIndex != other.endGeometryIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + (startGeometryIndex ?: 0)
        result = 31 * result + (endGeometryIndex ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlertGeometry(" +
            "length=$length, " +
            "shape=$shape, " +
            "startGeometryIndex=$startGeometryIndex, " +
            "endGeometryIndex=$endGeometryIndex"
    }

    /**
     * Use to create a new instance.
     *
     * @see RoadObjectGeometry
     */
    class Builder(
        private val length: Double?,
        private val shape: Geometry,
        private val startGeometryIndex: Int?,
        private val endGeometryIndex: Int?,
    ) {

        /**
         * Build the object instance.
         */
        fun build() = RoadObjectGeometry(
            length,
            shape,
            startGeometryIndex,
            endGeometryIndex
        )
    }
}
