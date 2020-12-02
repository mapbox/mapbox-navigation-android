package com.mapbox.navigation.ui.maps.route.line.model

/**
 *
 * @param distanceFromOrigin
 * @param trafficCongestionIdentifier
 * @param roadClass
 */
data class RouteLineTrafficExpressionData(
    val distanceFromOrigin: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String?
)
