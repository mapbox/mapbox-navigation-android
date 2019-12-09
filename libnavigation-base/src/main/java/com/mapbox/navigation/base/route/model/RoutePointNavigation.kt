package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Point

data class RoutePointNavigation(
    val point: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)
