package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.STRAIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.maneuver.model.LaneTurns

internal class LaneIconHelper {

    private val laneIcon: Map<LaneTurns, Int> = mapOf(
        LaneTurns.LANE_UTURN to R.drawable.mapbox_ic_uturn,
        LaneTurns.LANE_STRAIGHT to R.drawable.mapbox_ic_turn_straight,
        LaneTurns.LANE_RIGHT to R.drawable.mapbox_ic_turn_right,
        LaneTurns.LANE_SHARP_RIGHT to R.drawable.mapbox_ic_turn_sharp_right,
        LaneTurns.LANE_SLIGHT_RIGHT to R.drawable.mapbox_ic_turn_slight_right,
        LaneTurns.LANE_LEFT to R.drawable.mapbox_ic_turn_left,
        LaneTurns.LANE_SHARP_LEFT to R.drawable.mapbox_ic_turn_sharp_left,
        LaneTurns.LANE_SLIGHT_LEFT to R.drawable.mapbox_ic_turn_slight_left,
        LaneTurns.LANE_LEFT_STRAIGHT_LEFT_ONLY to
            R.drawable.mapbox_ic_lane_left_straight_left_only,
        LaneTurns.LANE_RIGHT_STRAIGHT_RIGHT_ONLY to
            R.drawable.mapbox_ic_lane_right_straight_right_only,
        LaneTurns.LANE_LEFT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_left_straight_straight_only,
        LaneTurns.LANE_RIGHT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_right_straight_straight_only,
        LaneTurns.LANE_SLIGHT_LEFT_STRAIGHT_SLIGHT_LEFT_ONLY to
            R.drawable.mapbox_ic_lane_slight_left_straight_slight_left_only,
        LaneTurns.LANE_SLIGHT_RIGHT_STRAIGHT_SLIGHT_RIGHT_ONLY to
            R.drawable.mapbox_ic_lane_slight_right_straight_slight_right_only,
        LaneTurns.LANE_SLIGHT_LEFT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_slight_left_straight_straight_only,
        LaneTurns.LANE_SLIGHT_RIGHT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_slight_right_straight_straight_only,
        LaneTurns.LANE_SHARP_LEFT_STRAIGHT_SHARP_LEFT_ONLY to
            R.drawable.mapbox_ic_lane_sharp_left_straight_sharp_left_only,
        LaneTurns.LANE_SHARP_RIGHT_STRAIGHT_SHARP_RIGHT_ONLY to
            R.drawable.mapbox_ic_lane_sharp_right_straight_sharp_right_only,
        LaneTurns.LANE_SHARP_LEFT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_sharp_left_straight_straight_only,
        LaneTurns.LANE_SHARP_RIGHT_STRAIGHT_STRAIGHT_ONLY to
            R.drawable.mapbox_ic_lane_sharp_right_straight_straight_only,
    )

    fun retrieveLaneToDraw(laneIndicator: LaneIndicator, activeDirection: String?): Int? {
        return when {
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == UTURN -> {
                laneIcon[LaneTurns.LANE_UTURN]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == LEFT -> {
                laneIcon[LaneTurns.LANE_LEFT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == RIGHT -> {
                laneIcon[LaneTurns.LANE_RIGHT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == STRAIGHT -> {
                laneIcon[LaneTurns.LANE_STRAIGHT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == SLIGHT_LEFT -> {
                laneIcon[LaneTurns.LANE_SLIGHT_LEFT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == SLIGHT_RIGHT -> {
                laneIcon[LaneTurns.LANE_SLIGHT_RIGHT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == SHARP_LEFT -> {
                laneIcon[LaneTurns.LANE_SHARP_LEFT]
            }
            laneIndicator.directions.size == 1 && laneIndicator.directions[0] == SHARP_RIGHT -> {
                laneIcon[LaneTurns.LANE_SHARP_RIGHT]
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(LEFT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_LEFT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_LEFT_STRAIGHT_LEFT_ONLY]
                    }
                }
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(RIGHT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_RIGHT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_RIGHT_STRAIGHT_RIGHT_ONLY]
                    }
                }
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(SLIGHT_RIGHT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_SLIGHT_RIGHT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_SLIGHT_RIGHT_STRAIGHT_SLIGHT_RIGHT_ONLY]
                    }
                }
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(SLIGHT_LEFT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_SLIGHT_LEFT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_SLIGHT_LEFT_STRAIGHT_SLIGHT_LEFT_ONLY]
                    }
                }
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(SHARP_RIGHT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_SHARP_RIGHT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_SHARP_RIGHT_STRAIGHT_SHARP_RIGHT_ONLY]
                    }
                }
            }
            laneIndicator.directions.size > 1 &&
                laneIndicator.directions.contains(STRAIGHT) &&
                laneIndicator.directions.contains(SHARP_LEFT) -> {
                activeDirection?.let {
                    if (it == STRAIGHT) {
                        laneIcon[LaneTurns.LANE_SHARP_LEFT_STRAIGHT_STRAIGHT_ONLY]
                    } else {
                        laneIcon[LaneTurns.LANE_SHARP_LEFT_STRAIGHT_SHARP_LEFT_ONLY]
                    }
                }
            }
            else -> null
        }
    }
}
