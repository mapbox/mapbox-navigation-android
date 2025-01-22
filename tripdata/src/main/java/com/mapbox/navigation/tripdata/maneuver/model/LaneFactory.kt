package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [Lane] object.
 */
@ExperimentalMapboxNavigationAPI
object LaneFactory {

    /**
     * Build [Lane] given appropriate arguments
     */
    @JvmStatic
    fun buildLane(
        allLanes: List<LaneIndicator>,
    ) = Lane(allLanes)
}
