package com.mapbox.navigation.base.route.model

import com.mapbox.api.directions.v5.models.LegStep

data class LegStepNavigation(
    val distance: Double?,
    val duration: Double?,
    val geometry: String?,
    val name: String?,
    val ref: String?,
    val destinations: String?,
    val mode: String?,
    val pronunciation: String?,
    val rotaryName: String?,
    val rotaryPronunciation: String?,
    val maneuver: StepManeuverNavigation?
)

fun LegStep.mapToStep() = LegStepNavigation(
    distance = distance(),
    duration = duration(),
    geometry = geometry(),
    name = name(),
    ref = ref(),
    destinations = destinations(),
    mode = mode(),
    pronunciation = pronunciation(),
    rotaryName = rotaryName(),
    rotaryPronunciation = rotaryPronunciation(),
    maneuver = maneuver().mapToManeuverStep()
)
