package com.mapbox.navigation.ui.androidauto.navigation.lanes

import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection

internal class CarLaneMapper {

    fun mapLanes(laneGuidance: com.mapbox.navigation.tripdata.maneuver.model.Lane): List<Lane> {
        return laneGuidance.allLanes.map { laneIndicator ->
            val laneBuilder = Lane.Builder()
            laneIndicator.directions.forEach { indicator ->
                val shape = LANE_DIRECTION_MAP[indicator]
                checkNotNull(shape) { "Was unable to map $indicator for lane guidance" }
                val laneDirection = LaneDirection.create(shape, laneIndicator.isActive)
                laneBuilder.addDirection(laneDirection)
            }
            laneBuilder.build()
        }
    }

    companion object {
        val LANE_DIRECTION_MAP = mapOf(
            "none" to LaneDirection.SHAPE_UNKNOWN,
            "straight" to LaneDirection.SHAPE_STRAIGHT,
            "left" to LaneDirection.SHAPE_NORMAL_LEFT,
            "slight left" to LaneDirection.SHAPE_SLIGHT_LEFT,
            "sharp left" to LaneDirection.SHAPE_SHARP_LEFT,
            "right" to LaneDirection.SHAPE_NORMAL_RIGHT,
            "slight right" to LaneDirection.SHAPE_SLIGHT_RIGHT,
            "sharp right" to LaneDirection.SHAPE_SHARP_RIGHT,
            "uturn" to LaneDirection.SHAPE_U_TURN_LEFT,
        )
    }
}
