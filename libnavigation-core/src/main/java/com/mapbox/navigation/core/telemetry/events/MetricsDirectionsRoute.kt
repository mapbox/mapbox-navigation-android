package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.telemetry.obtainStepCount

internal data class MetricsDirectionsRoute(
    val stepCount: Int,
    val distance: Int,
    val duration: Int,
    val requestIdentifier: String?,
    val geometry: String?,
) {

    constructor(route: NavigationRoute?) : this(
        obtainStepCount(route?.directionsRoute),
        distance = route?.directionsRoute?.distance()?.toInt() ?: 0,
        duration = route?.directionsRoute?.duration()?.toInt() ?: 0,
        route?.directionsResponse?.uuid(),
        route?.directionsRoute?.geometry(),
    )
}
