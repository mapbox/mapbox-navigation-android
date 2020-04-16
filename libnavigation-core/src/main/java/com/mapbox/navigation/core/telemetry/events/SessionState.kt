package com.mapbox.navigation.core.telemetry.events

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.telemetry.obtainGeometry
import com.mapbox.navigation.core.telemetry.obtainStepCount
import java.util.Date

internal data class SessionState(
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

    // TODO:OZ Clean up never used functions below
    //  We should also review SessionState properties as some of them are set but never used
    //  E.g. percentInForeground and percentInPortrait set twice
    //  see MapboxNavigationTelemetry#populateSessionState and MapboxNavigationTelemetry#populateNavigationEvent
    //  and never referenced in RerouteEvent / NavigationRerouteEvent
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
}
