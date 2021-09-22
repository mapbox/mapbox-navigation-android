package com.mapbox.navigation.core.routealternatives

import com.mapbox.geojson.Point

data class RouteIntersection internal constructor(
    val point: Point,
    val segmentIndex: Int,
    val legIndex: Int
)
