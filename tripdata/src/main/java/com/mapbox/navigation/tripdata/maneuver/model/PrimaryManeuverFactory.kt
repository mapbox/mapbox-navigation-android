package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [PrimaryManeuver] object.
 */
@ExperimentalMapboxNavigationAPI
object PrimaryManeuverFactory {

    /**
     * Build [PrimaryManeuver] given appropriate arguments.
     */
    @JvmStatic
    fun buildPrimaryManeuver(
        id: String,
        text: String,
        type: String?,
        degrees: Double?,
        modifier: String?,
        drivingSide: String?,
        componentList: List<Component>,
    ) = PrimaryManeuver(id, text, type, degrees, modifier, drivingSide, componentList)
}
