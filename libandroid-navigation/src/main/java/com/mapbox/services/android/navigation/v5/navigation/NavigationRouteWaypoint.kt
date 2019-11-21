package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.geojson.Point

internal data class NavigationRouteWaypoint(
    val waypoint: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)
