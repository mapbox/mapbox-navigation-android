package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.location.Location
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.Date

data class SessionState(
    var secondsSinceLastReroute: Int = -1,
    var eventRouteProgress: MetricsRouteProgress = MetricsRouteProgress(null),
    var eventLocation: Location = Location(MetricsLocation.PROVIDER).apply {
        latitude = 0.0
        longitude = 0.0
    },
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
    fun originalStepCount(): Int = obtainStepCount(originalDirectionRoute)

    fun originalGeometry(): String = obtainGeometry(originalDirectionRoute)

    fun originalDistance(): Int = originalDirectionRoute?.distance()?.toInt() ?: 0

    fun originalDuration(): Int = originalDirectionRoute?.duration()?.toInt() ?: 0

    /*
    * Current route values
    */
    fun currentStepCount(): Int = obtainStepCount(currentDirectionRoute)

    fun currentGeometry(): String = obtainGeometry(currentDirectionRoute)

    private fun obtainGeometry(directionsRoute: DirectionsRoute?): String =
        ifNonNull(directionsRoute, directionsRoute?.geometry()) { _, geometry ->
            if (TextUtils.isEmpty(geometry)) {
                return@ifNonNull ""
            }
            val positions = PolylineUtils.decode(geometry, Constants.PRECISION_6)
            return@ifNonNull PolylineUtils.encode(positions, Constants.PRECISION_5)
        } ?: ""

    private fun obtainStepCount(directionsRoute: DirectionsRoute?): Int =
        ifNonNull(directionsRoute, directionsRoute?.legs()) { _, legs ->
            var stepCount = 0
            for (leg in legs) {
                stepCount += leg.steps()?.size ?: 0
            }
            return@ifNonNull stepCount
        } ?: 0
}
