package com.mapbox.navigation.ui.androidauto.navigation.lanes

import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure

internal class CarLaneMapper {

    // Lanes are mapped on every route progress update, so each unknown indication is logged once.
    private val loggedUnknownIndications = mutableSetOf<String>()

    fun mapLanes(laneGuidance: com.mapbox.navigation.tripdata.maneuver.model.Lane): List<Lane> {
        return laneGuidance.allLanes.map { laneIndicator ->
            val laneBuilder = Lane.Builder()
            // Only the direction of an active lane that the route takes is recommended. Without
            // an active direction, which some profiles do not provide, every direction is.
            val activeDirection = laneIndicator.activeDirection
            laneIndicator.directions.forEach { indicator ->
                val isRecommended = laneIndicator.isActive &&
                    (activeDirection == null || indicator == activeDirection)
                val laneDirection = LaneDirection.create(
                    shapeFor(indicator, laneIndicator.drivingSide),
                    isRecommended,
                )
                laneBuilder.addDirection(laneDirection)
            }
            laneBuilder.build()
        }
    }

    private fun shapeFor(indicator: String, drivingSide: String): Int {
        // U-turns are made towards the centre of the road.
        if (indicator == U_TURN && drivingSide == DRIVING_SIDE_LEFT) {
            return LaneDirection.SHAPE_U_TURN_RIGHT
        }
        return LANE_DIRECTION_MAP[indicator] ?: unknownShape(indicator)
    }

    private fun unknownShape(indicator: String): Int {
        if (loggedUnknownIndications.add(indicator)) {
            logAndroidAutoFailure(
                "CarLaneMapper: unknown lane indication \"$indicator\", using SHAPE_UNKNOWN",
            )
        }
        return LaneDirection.SHAPE_UNKNOWN
    }

    companion object {
        private const val U_TURN = "uturn"
        private const val DRIVING_SIDE_LEFT = "left"

        val LANE_DIRECTION_MAP = mapOf(
            "none" to LaneDirection.SHAPE_UNKNOWN,
            "straight" to LaneDirection.SHAPE_STRAIGHT,
            "left" to LaneDirection.SHAPE_NORMAL_LEFT,
            "slight left" to LaneDirection.SHAPE_SLIGHT_LEFT,
            "sharp left" to LaneDirection.SHAPE_SHARP_LEFT,
            "right" to LaneDirection.SHAPE_NORMAL_RIGHT,
            "slight right" to LaneDirection.SHAPE_SLIGHT_RIGHT,
            "sharp right" to LaneDirection.SHAPE_SHARP_RIGHT,
            U_TURN to LaneDirection.SHAPE_U_TURN_LEFT,
        )
    }
}
