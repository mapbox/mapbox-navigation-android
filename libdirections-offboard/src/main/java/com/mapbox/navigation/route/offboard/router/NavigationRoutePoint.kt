package com.mapbox.navigation.route.offboard.router

import com.mapbox.geojson.Point

internal data class NavigationRoutePoint(
    val point: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)
