package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.maneuver.LaneIconProcessor
import com.mapbox.navigation.ui.maneuver.model.LaneIcon
import com.mapbox.navigation.ui.maneuver.model.LaneIconError
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.view.MapboxLaneGuidance
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * MapboxLaneIconsApi allows you to request the lane icon from [LaneIndicator]
 * in the form of vector drawables which can then be styled and rendered
 * using [MapboxLaneGuidance] or your own custom view.
*/
class MapboxLaneIconsApi {

    /**
     * Provide a styleable vector drawable resource of the lane indicator.
     * @param laneIndicator LaneIndicator
     * @param activeDirection String?
     */
    fun laneIcon(
        laneIndicator: LaneIndicator,
        activeDirection: String?
    ): Expected<LaneIconError, LaneIcon> {
        val drawableResId = LaneIconProcessor.getDrawableFrom(laneIndicator, activeDirection)
        return ifNonNull(drawableResId) {
            ExpectedFactory.createValue(LaneIcon(it))
        } ?: ExpectedFactory.createError(createUnrecognizedError(laneIndicator, activeDirection))
    }

    private fun createUnrecognizedError(
        laneIndicator: LaneIndicator,
        activeDirection: String?
    ): LaneIconError {
        val errorMessage = "Unrecognized lane $laneIndicator activeDirection: $activeDirection"
        return LaneIconError(errorMessage, laneIndicator, activeDirection)
    }
}
