package com.mapbox.navigation.ui.maneuver.model

/**
 * Represents an error value for an lane icon request.
 *
 * @param errorMessage an error message
 * @param laneIndicator the lane description
 * @param activeDirection if that lane can be used to complete the upcoming maneuver
 */
class LaneIconError internal constructor(
    val errorMessage: String,
    val laneIndicator: LaneIndicator,
    val activeDirection: String?
)
