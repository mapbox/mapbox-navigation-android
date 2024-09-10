package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build [SubManeuver] object.
 */
@ExperimentalMapboxNavigationAPI
object SubManeuverFactory {

    /**
     * Build [SubManeuver] given appropriate arguments.
     */
    @JvmStatic
    fun buildSubManeuver(
        id: String,
        text: String,
        type: String?,
        degrees: Double?,
        modifier: String?,
        drivingSide: String?,
        componentList: List<Component>,
    ) = SubManeuver(id, text, type, degrees, modifier, drivingSide, componentList)
}
