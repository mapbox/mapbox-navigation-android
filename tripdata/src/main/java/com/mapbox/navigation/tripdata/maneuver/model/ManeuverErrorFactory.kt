package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [ManeuverError] object.
 */
@ExperimentalMapboxNavigationAPI
object ManeuverErrorFactory {

    /**
     * Build [ManeuverError] given appropriate arguments
     */
    @JvmStatic
    fun buildManeuverError(
        errorMessage: String,
        throwable: Throwable?,
    ) = ManeuverError(errorMessage, throwable)
}
