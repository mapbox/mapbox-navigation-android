package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [SecondaryManeuver] object.
 */
@ExperimentalMapboxNavigationAPI
object SecondaryManeuverFactory {

    /**
     * Build [SecondaryManeuver] given appropriate arguments.
     */
    @JvmStatic
    fun buildSecondaryManeuver(
        id: String,
        text: String,
        type: String?,
        degrees: Double?,
        modifier: String?,
        drivingSide: String?,
        componentList: List<Component>,
    ) = SecondaryManeuver(id, text, type, degrees, modifier, drivingSide, componentList)
}
