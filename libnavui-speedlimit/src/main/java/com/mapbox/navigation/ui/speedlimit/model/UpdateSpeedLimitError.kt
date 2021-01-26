package com.mapbox.navigation.ui.speedlimit.model

/**
 * Represents an error value for trip progress updates.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class UpdateSpeedLimitError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?
)
