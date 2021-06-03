package com.mapbox.navigation.ui.maneuver.model

/**
 * Represents an error value for maneuver updates.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class ManeuverError internal constructor(
    val errorMessage: String?,
    val throwable: Throwable? = null
)
