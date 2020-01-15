package com.mapbox.navigation.trip.notification

const val STEP_MANEUVER_TYPE_TURN = "turn"
const val STEP_MANEUVER_TYPE_NEW_NAME = "new name"
const val STEP_MANEUVER_TYPE_DEPART = "depart"
const val STEP_MANEUVER_TYPE_ARRIVE = "arrive"
const val STEP_MANEUVER_TYPE_MERGE = "merge"
const val STEP_MANEUVER_TYPE_ON_RAMP = "on ramp"
const val STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp"
const val STEP_MANEUVER_TYPE_FORK = "fork"
const val STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road"
const val STEP_MANEUVER_TYPE_CONTINUE = "continue"
const val STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout"
const val STEP_MANEUVER_TYPE_ROTARY = "rotary"
const val STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn"
const val STEP_MANEUVER_TYPE_NOTIFICATION = "notification"
// TODO Why are these never used?
const val STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout"
const val STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary"

// Step Maneuver Modifiers
const val STEP_MANEUVER_MODIFIER_UTURN = "uturn"
const val STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right"
const val STEP_MANEUVER_MODIFIER_RIGHT = "right"
const val STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right"
const val STEP_MANEUVER_MODIFIER_STRAIGHT = "straight"
const val STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left"
const val STEP_MANEUVER_MODIFIER_LEFT = "left"
const val STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left"

internal object ManeuverResource {

    private val maneuverMap: Map<String, Int> = mapOf(
        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN to R.drawable.ic_maneuver_turn_180,
        STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN to R.drawable.ic_maneuver_turn_180,
        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_180_left_driving_side,
        STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_180_left_driving_side,

        STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_arrive_left,
        STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_arrive_right,
        STEP_MANEUVER_TYPE_ARRIVE to R.drawable.ic_maneuver_arrive,

        STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_depart_left,
        STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_depart_right,
        STEP_MANEUVER_TYPE_DEPART to R.drawable.ic_maneuver_depart,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT to R.drawable.ic_maneuver_turn_75,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT to R.drawable.ic_maneuver_turn_75,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT to R.drawable.ic_maneuver_turn_75,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_turn_45,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_turn_45,
        STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_turn_45,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_turn_45,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_turn_30,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_turn_30,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_turn_30,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT to R.drawable.ic_maneuver_turn_75_left,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT to R.drawable.ic_maneuver_turn_75_left,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT to R.drawable.ic_maneuver_turn_75_left,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_45_left,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_45_left,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_45_left,
        STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_turn_45_left,

        STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_turn_30_left,
        STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_turn_30_left,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_turn_30_left,

        STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_merge_left,
        STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_merge_left,

        STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_merge_right,
        STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_merge_right,

        STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_off_ramp_left,
        STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_off_ramp_slight_left,

        STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_off_ramp_right,
        STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_off_ramp_slight_right,

        STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_fork_left,
        STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_fork_slight_left,
        STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_fork_right,
        STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_fork_slight_right,
        STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_fork_straight,
        STEP_MANEUVER_TYPE_FORK to R.drawable.ic_maneuver_fork,

        STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_end_of_road_left,
        STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_end_of_road_right,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_left,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_left,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT to R.drawable.ic_maneuver_roundabout_sharp_left,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT to R.drawable.ic_maneuver_roundabout_sharp_left,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_roundabout_slight_left,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT to R.drawable.ic_maneuver_roundabout_slight_left,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_roundabout_right,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT to R.drawable.ic_maneuver_roundabout_right,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT to R.drawable.ic_maneuver_roundabout_sharp_right,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT to R.drawable.ic_maneuver_roundabout_sharp_right,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_roundabout_slight_right,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT to R.drawable.ic_maneuver_roundabout_slight_right,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_roundabout_straight,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_roundabout_straight,

        STEP_MANEUVER_TYPE_ROUNDABOUT to R.drawable.ic_maneuver_roundabout,
        STEP_MANEUVER_TYPE_ROTARY to R.drawable.ic_maneuver_roundabout,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_left_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_left_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_right_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_right_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side,

        STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_straight_left_driving_side,
        STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT to R.drawable.ic_maneuver_roundabout_straight_left_driving_side,

        STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_turn_0,
        STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_turn_0,
        STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_turn_0,
        STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT to R.drawable.ic_maneuver_turn_0
    )

    fun obtainManeuverResource(maneuver: String?): Int {
        return maneuverMap[maneuver] ?: R.drawable.ic_maneuver_turn_0
    }
}
