@file:JvmName("RouteAlertUtils")

package com.mapbox.navigation.base.trip.model.alert

import androidx.annotation.IntRange
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString

/**
 * Returns a [LineString] geometry of the alert.
 *
 * @param directionsRoute the original route that this alert is a part of.
 * @param precision precision of the polyline encoding.
 * Possible values are [Constants.PRECISION_5] and [Constants.PRECISION_6].
 * See [RouteOptions.Builder.geometries].
 */
fun RouteAlertGeometry.toLineString(
    directionsRoute: DirectionsRoute,
    @IntRange(from = 5, to = 6) precision: Int
): LineString {
    val routeLineString = LineString.fromPolyline(directionsRoute.geometry()!!, precision)
    return this.toLineString(routeLineString)
}

/**
 * Returns a [LineString] geometry of the alert.
 *
 * @param routeLineString line-string of the original route that this alert is a part of.
 */
fun RouteAlertGeometry.toLineString(routeLineString: LineString): LineString {
    return LineString.fromLngLats(
        routeLineString.coordinates().subList(
            this.startGeometryIndex,
            this.endGeometryIndex
        )
    )
}
