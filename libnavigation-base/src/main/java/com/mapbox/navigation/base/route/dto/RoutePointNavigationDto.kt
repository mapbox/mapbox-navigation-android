package com.mapbox.navigation.base.route.dto

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.RoutePointNavigation

data class RoutePointNavigationDto(
    val point: Point,
    val bearingAngle: Double?,
    val tolerance: Double?
)

fun RoutePointNavigationDto.mapToModel() = RoutePointNavigation(
    point = point,
    bearingAngle = bearingAngle,
    tolerance = tolerance
)
