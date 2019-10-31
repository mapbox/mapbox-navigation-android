package com.mapbox.navigation.route.common

import com.mapbox.geojson.Point

data class NavigationRouteWaypoint(
    val waypoint: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)
