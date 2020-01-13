package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.model.StepManeuverNavigation
import com.mapbox.navigation.base.route.model.StepManeuverType

class StepManeuverNavigationDto(
    @SerializedName("location")
    val rawLocation: DoubleArray,
    @SerializedName("bearing_before")
    val bearingBefore: Double?,
    @SerializedName("bearing_after")
    val bearingAfter: Double?,
    val instruction: String?,
    @StepManeuverType
    val type: String?,
    val modifier: String?,
    val exit: Int?
)

fun StepManeuverNavigationDto.mapToModel() = StepManeuverNavigation.Builder()
    .modifier(modifier)
    .type(type)
    .build()
