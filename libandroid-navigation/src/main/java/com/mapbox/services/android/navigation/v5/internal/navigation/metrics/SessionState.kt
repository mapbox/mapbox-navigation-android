package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.location.Location
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.Date

internal data class SessionState @JvmOverloads constructor(
    var secondsSinceLastReroute: Int = -1,
    var eventRouteProgress: MetricsRouteProgress = MetricsRouteProgress(null),
    var eventLocation: Location? = null,
    var eventDate: Date? = null,
    var eventRouteDistanceCompleted: Double = 0.0,
    var afterEventLocations: List<Location>? = null,
    var beforeEventLocations: List<Location>? = null,
    var originalDirectionRoute: DirectionsRoute? = null,
    var currentDirectionRoute: DirectionsRoute? = null,
    var sessionIdentifier: String = "",
    var tripIdentifier: String = "",
    var originalRequestIdentifier: String? = null,
    var requestIdentifier: String? = null,
    var mockLocation: Boolean = false,
    var rerouteCount: Int = 0,
    var startTimestamp: Date? = null,
    var arrivalTimestamp: Date? = null,
    var locationEngineName: String = "",
    var percentInForeground: Int = 100,
    var percentInPortrait: Int = 100
) {

    /*
    * Original route values
    */
    fun originalGeometry(): String =
        ifNonNull(originalDirectionRoute, originalDirectionRoute?.geometry()) { _, geometry ->
            calculateGeometry(geometry)
        } ?: ""

    fun originalDistance(): Int =
        originalDirectionRoute?.distance()?.toInt() ?: 0

    fun originalStepCount(): Int =
        ifNonNull(originalDirectionRoute, originalDirectionRoute?.legs()) { _, legs ->
            calculateStepCount(legs)
        } ?: 0

    fun originalDuration(): Int =
        originalDirectionRoute?.duration()?.toInt() ?: 0

    /*
    * Current route values
    */
    fun currentStepCount(): Int =
        ifNonNull(currentDirectionRoute, currentDirectionRoute?.legs()) { _, legs ->
            calculateStepCount(legs)
        } ?: 0

    fun currentGeometry(): String =
        ifNonNull(currentDirectionRoute, currentDirectionRoute?.geometry()) { _, geometry ->
            calculateGeometry(geometry)
        } ?: ""

    private fun calculateStepCount(legs: List<RouteLeg>): Int {
        var stepCount = 0
        for (leg in legs) {
            stepCount += leg.steps()?.size ?: 0
        }
        return stepCount
    }

    private fun calculateGeometry(geometry: String): String {
        if (TextUtils.isEmpty(geometry)) {
            return ""
        }
        val positions = PolylineUtils.decode(geometry, Constants.PRECISION_6)
        return PolylineUtils.encode(positions, Constants.PRECISION_5)
    }
}
