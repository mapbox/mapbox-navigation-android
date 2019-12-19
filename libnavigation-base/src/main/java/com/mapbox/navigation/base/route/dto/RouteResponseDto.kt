package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.RouteResponse

class RouteResponseDto(
    val message: String?,
    val code: String?,
    val uuid: String?,
    val routes: List<RouteDto>?
)

fun RouteResponseDto.mapToModel() = RouteResponse(
    message = message,
    code = code,
    uuid = uuid,
    routes = routes?.map(RouteDto::mapToModelRoute)
)
