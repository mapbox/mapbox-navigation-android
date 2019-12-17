package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.StepManeuverNavigation

 class StepManeuverNavigationDto(
    @SerializedName("location")
    val rawLocation: DoubleArray,
    @SerializedName("bearing_before")
    val bearingBefore: Double?,
    @SerializedName("bearing_after")
    val bearingAfter: Double?,
    val instruction: String?,
    @StepManeuverNavigation.StepManeuverTypeNavigation
    val type: String?,
    val modifier: String?,
    val exit: Int?
)

 fun StepManeuverNavigationDto.mapToModel() = StepManeuverNavigation(
    location = Point.fromLngLat(rawLocation[0], rawLocation[1]),
    bearingBefore = bearingBefore,
    bearingAfter = bearingAfter,
    instruction = instruction,
    type = type,
    modifier = modifier,
    exit = exit
)
