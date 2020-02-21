package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.trip.model.RouteProgress

internal data class RouteProgressWithTimeStamp(val date: Long, val routeProgress: RouteProgress)
