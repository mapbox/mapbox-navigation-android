package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.navigation.base.route.model.StepManeuverNavigation

internal class LegStepNavigationDto(
    val distance: Double,
    val duration: Double,
    val geometry: String?,
    val name: String?,
    val ref: String?,
    val destinations: String?,
    val mode: String,
    val pronunciation: String?,
    @SerializedName("rotary_name")
    val rotaryName: String?,
    @SerializedName("rotary_pronunciation")
    val rotaryPronunciation: String?,
    val maneuver: StepManeuverNavigation,
    val voiceInstructions: List<VoiceInstructionsNavigationDto>?,
    val bannerInstructions: List<BannerInstructionsNavigationDto>?,
    @SerializedName("driving_side")
    val drivingSide: String?,
    val weight: Double,
    val intersections: List<StepIntersectionNavigationDto>?,
    val exits: String?
)

internal fun LegStepNavigationDto.mapToModel() = LegStepNavigation(
    distance = distance,
    duration = duration,
    geometry = geometry,
    name = name,
    ref = ref,
    destinations = destinations,
    mode = mode,
    pronunciation = pronunciation,
    rotaryName = rotaryName,
    rotaryPronunciation = rotaryPronunciation,
    maneuver = maneuver,
    voiceInstructions = voiceInstructions?.map(VoiceInstructionsNavigationDto::mapToModel),
    bannerInstructions = bannerInstructions?.map(BannerInstructionsNavigationDto::mapToModel),
    drivingSide = drivingSide,
    weight = weight,
    intersections = intersections?.map(StepIntersectionNavigationDto::mapToModel),
    exits = exits
)
