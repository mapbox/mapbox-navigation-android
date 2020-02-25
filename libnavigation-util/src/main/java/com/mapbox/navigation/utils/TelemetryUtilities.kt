package com.mapbox.navigation.utils

import android.location.Location
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.floor

private fun calculateScreenBrightnessPercentage(screenBrightness: Int): Int =
        floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX).toInt()

fun obtainGeometry(directionsRoute: DirectionsRoute?): String =
        ifNonNull(directionsRoute, directionsRoute?.geometry()) { _, geometry ->
            if (TextUtils.isEmpty(geometry)) {
                return@ifNonNull ""
            }
            val positions = PolylineUtils.decode(geometry, PRECISION_6)
            return@ifNonNull PolylineUtils.encode(positions, PRECISION_5)
        } ?: ""

fun obtainStepCount(directionsRoute: DirectionsRoute?): Int =
        ifNonNull(directionsRoute, directionsRoute?.legs()) { _, legs ->
            var stepCount = 0
            for (leg in legs) {
                stepCount += leg.steps()?.size ?: 0
            }
            return@ifNonNull stepCount
        } ?: 0

fun obtainAbsoluteDistance(
    currentLocation: Location,
    finalPoint: Point
): Int {
    val currentPoint = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
    return TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS)
            .toInt()
}

fun obtainRouteDestination(route: DirectionsRoute): Point =
        route.legs()?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
                ?: Point.fromLngLat(0.0, 0.0)
