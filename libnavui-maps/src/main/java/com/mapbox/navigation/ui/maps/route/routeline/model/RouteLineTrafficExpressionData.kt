package com.mapbox.navigation.ui.maps.route.routeline.model

data class RouteLineTrafficExpressionData(
    val distanceFromOrigin: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String?
)
