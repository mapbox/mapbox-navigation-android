package com.mapbox.navigation.ui.maps.route.line.model

internal data class RouteLineTrafficExpressionData(
    val distanceFromOrigin: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String?
)
