package com.mapbox.navigation.base.route.model

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
