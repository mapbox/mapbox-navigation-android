package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.maneuver.LaneIconProcessor
import com.mapbox.navigation.ui.maneuver.model.LaneIcon
import com.mapbox.navigation.ui.maneuver.model.LaneIconResources
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.view.MapboxLaneGuidance

/**
 * MapboxLaneIconsApi allows you to request the lane icon from [LaneIndicator]
 * in the form of vector drawables which can then be styled and rendered
 * using [MapboxLaneGuidance] or your own custom view.
 * @param laneIconProcessor LaneIconProcessor
*/
class MapboxLaneIconsApi private constructor(
    private val laneIconProcessor: LaneIconProcessor
) {

    /**
     * @param laneIconResources LaneIconResources a [Builder] that allows you to specify your own
     * set of turn lane icons.
     */
    @JvmOverloads constructor(
        laneIconResources: LaneIconResources = LaneIconResources.Builder().build()
    ) : this(
        LaneIconProcessor(laneIconResources)
    )

    /**
     * Provide a styleable vector drawable resource of the lane indicator.
     * @param laneIndicator LaneIndicator
     */
    fun getTurnLane(laneIndicator: LaneIndicator): LaneIcon {
        return laneIconProcessor.getLaneIcon(laneIndicator)
    }
}
