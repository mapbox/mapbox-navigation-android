package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.base.trip.model.alert.RouteAlert

internal data class RouteInitInfo(
    val routeAlerts: List<RouteAlert>
)
