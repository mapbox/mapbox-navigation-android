package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.LegAnnotationNavigation

internal class LegAnnotationNavigationDto(
    val distance: List<Double>?,
    val duration: List<Double>?,
    val speed: List<Double>?,
    val maxspeed: List<MaxSpeedNavigationDto>?,
    val congestion: List<String>?
)

internal fun LegAnnotationNavigationDto.mapToModel() = LegAnnotationNavigation(
    distance = distance,
    duration = duration,
    speed = speed,
    maxspeed = maxspeed?.map(MaxSpeedNavigationDto::mapToModel),
    congestion = congestion
)
