package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.base.trip.model.alert.RouteAlert

data class RouteInitInfo(
    val routeAlerts: List<RouteAlert>
)
