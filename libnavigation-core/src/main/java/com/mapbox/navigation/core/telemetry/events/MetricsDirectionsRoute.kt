package com.mapbox.navigation.core.telemetry.events

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.telemetry.obtainStepCount

internal data class MetricsDirectionsRoute(
    val stepCount: Int,
    val distance: Int,
    val duration: Int,
    val requestIdentifier: String?,
    val geometry: String?,
) {

    constructor(route: DirectionsRoute?) : this(
        obtainStepCount(route),
        distance = route?.distance()?.toInt() ?: 0,
        duration = route?.duration()?.toInt() ?: 0,
        route?.requestUuid(),
        route?.geometry(),
    )
}
