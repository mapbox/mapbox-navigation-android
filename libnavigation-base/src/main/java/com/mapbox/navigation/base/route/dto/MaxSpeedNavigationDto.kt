package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.MaxSpeedNavigation

class MaxSpeedNavigationDto(
    val speed: Int?,
    val unit: String?,
    val unknown: Boolean?,
    val none: Boolean?
)

fun MaxSpeedNavigationDto.mapToModel() = MaxSpeedNavigation(
    speed = speed,
    unit = unit,
    unknown = unknown,
    none = none
)
