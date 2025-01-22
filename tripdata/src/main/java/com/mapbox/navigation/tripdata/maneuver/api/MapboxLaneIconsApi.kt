package com.mapbox.navigation.tripdata.maneuver.api

import com.mapbox.navigation.tripdata.maneuver.LaneIconProcessor
import com.mapbox.navigation.tripdata.maneuver.model.LaneIcon
import com.mapbox.navigation.tripdata.maneuver.model.LaneIconResources
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator

/**
 * MapboxLaneIconsApi allows you to request the lane icon from [LaneIndicator]
 * in the form of vector drawables which can then be styled and rendered
 * using your own custom view.
 * @param laneIconProcessor LaneIconProcessor
*/
class MapboxLaneIconsApi private constructor(
    private val laneIconProcessor: LaneIconProcessor,
) {

    /**
     * @param laneIconResources LaneIconResources a [LaneIconResources.Builder] that allows you to specify your own
     * set of turn lane icons.
     */
    @JvmOverloads constructor(
        laneIconResources: LaneIconResources = LaneIconResources.Builder().build(),
    ) : this(
        LaneIconProcessor(laneIconResources),
    )

    /**
     * Provide a styleable vector drawable resource of the lane indicator.
     * @param laneIndicator LaneIndicator
     */
    fun getTurnLane(laneIndicator: LaneIndicator): LaneIcon {
        return laneIconProcessor.getLaneIcon(laneIndicator)
    }
}
