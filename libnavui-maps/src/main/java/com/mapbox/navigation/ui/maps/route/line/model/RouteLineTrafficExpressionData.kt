package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Traffic related data for section of a route
 *
 * @param distanceFromOrigin the distance from the route origin
 * @param trafficCongestionIdentifier a string indicating the level of traffic congestion
 * @param roadClass an optional road class for route section
 */
internal data class RouteLineTrafficExpressionData(
    val distanceFromOrigin: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String?,
    val isInRestrictedSection: Boolean
)
