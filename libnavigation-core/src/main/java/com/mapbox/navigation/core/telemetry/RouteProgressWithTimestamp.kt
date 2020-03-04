package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.trip.model.RouteProgress

internal data class RouteProgressWithTimestamp(val date: Long, val routeProgress: RouteProgress)
