@file:JvmName("RoadObjectUtils")

package com.mapbox.navigation.base.trip.model.roadobject

import androidx.annotation.IntRange
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.utils.ifNonNull

private const val PRECISION_RANGE_START = 5L
private const val PRECISION_RANGE_END = 6L

/**
 * Returns a [LineString] geometry of the road object.
 *
 * @param directionsRoute the original route that this object is a part of.
 * @param precision precision of the polyline encoding.
 * Possible values are [Constants.PRECISION_5] and [Constants.PRECISION_6].
 * See [RouteOptions.Builder.geometries].
 */
fun RoadObjectGeometry.toLineString(
    directionsRoute: DirectionsRoute,
    @IntRange(from = PRECISION_RANGE_START, to = PRECISION_RANGE_END) precision: Int
): LineString? {
    if (precision !in PRECISION_RANGE_START..PRECISION_RANGE_END) {
        throw IllegalArgumentException(
            "Precision must be in $PRECISION_RANGE_START..$PRECISION_RANGE_END"
        )
    }
    val routeLineString = LineString.fromPolyline(directionsRoute.geometry()!!, precision)
    return this.toLineString(routeLineString)
}

/**
 * Returns a [LineString] geometry of the road object.
 *
 * @param routeLineString line-string of the original route that this object is a part of.
 */
fun RoadObjectGeometry.toLineString(routeLineString: LineString): LineString? {
    return ifNonNull(startGeometryIndex, endGeometryIndex) { start, end ->
        LineString.fromLngLats(
            routeLineString.coordinates().slice(start..end)
        )
    }
}
