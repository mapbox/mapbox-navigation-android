package com.mapbox.navigation.route

import com.mapbox.geojson.Point

internal data class NavigationRouteWaypoint(
    val point: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)
