package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.StepIntersectionNavigation

class StepIntersectionNavigationDto(
    @SerializedName("location")
    val rawLocation: DoubleArray,
    val bearings: List<Int>?,
    val classes: List<String>?,
    val entry: List<Boolean>?,
    val `in`: Int?,
    val out: Int?,
    val lanes: List<IntersectionLanesNavigationDto>?
)

fun StepIntersectionNavigationDto.mapToModel() = StepIntersectionNavigation(
    location = Point.fromLngLat(rawLocation[0], rawLocation[1]),
    bearings = bearings,
    classes = classes,
    entry = entry,
    into = `in`,
    out = out,
    lanes = lanes?.map(IntersectionLanesNavigationDto::mapToModel)
)
