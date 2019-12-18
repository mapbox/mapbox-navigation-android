package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.RouteResponse

class RouteResponseDto(val routes: List<RouteDto>?)

fun RouteResponseDto.mapToModel() = RouteResponse(
    routes?.map(RouteDto::mapToModelRoute)
)
