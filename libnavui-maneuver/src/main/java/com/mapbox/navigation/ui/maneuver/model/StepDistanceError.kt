package com.mapbox.navigation.ui.maneuver.model

/**
 * Represents an error value for step distance updates.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class StepDistanceError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?
)
