package com.mapbox.navigation.base.internal.maneuver

import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.STRAIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.api.directions.v5.models.StepManeuver.ARRIVE
import com.mapbox.api.directions.v5.models.StepManeuver.DEPART
import com.mapbox.api.directions.v5.models.StepManeuver.END_OF_ROAD
import com.mapbox.api.directions.v5.models.StepManeuver.EXIT_ROTARY
import com.mapbox.api.directions.v5.models.StepManeuver.EXIT_ROUNDABOUT
import com.mapbox.api.directions.v5.models.StepManeuver.FORK
import com.mapbox.api.directions.v5.models.StepManeuver.MERGE
import com.mapbox.api.directions.v5.models.StepManeuver.OFF_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ON_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ROTARY
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT_TURN
import com.mapbox.api.directions.v5.models.StepManeuver.TURN
import com.mapbox.navigation.base.maneuver.model.BaseTurnIconResources
import kotlin.math.roundToInt

/**
 * Shared Helper class that maps Maneuver to Icon.
 */
class TurnIconHelper(
    turnIconResources: BaseTurnIconResources,
) {

    private val defaultTurnIcon = icon(turnIconResources.turnIconTurnStraight)

    // source of truth for all Maneuver to icon mappings
    private val iconMap: Map<ManeuverTypeModifierPair, IconSpec> = turnIconResources.run {
        mapOf(
            // When type == null and modifier == null
            maneuver(null, null) to icon(turnIconTurnStraight),
            maneuver("", "") to icon(turnIconTurnStraight),

            // When type != null and modifier == null
            maneuver(ARRIVE, null) to icon(turnIconArrive),
            maneuver(DEPART, null) to icon(turnIconDepart),
            maneuver(ON_RAMP, null) to icon(turnIconOnRamp),
            maneuver(OFF_RAMP, null) to icon(turnIconOffRamp, true),
            maneuver(FORK, null) to icon(turnIconFork, true),
            maneuver(TURN, null) to icon(turnIconTurnStraight),
            maneuver(MERGE, null) to icon(turnIconMergeStraight),
            maneuver(END_OF_ROAD, null) to icon(turnIconEndRoadLeft),

            // When type = null and modifier != null
            maneuver(null, LEFT) to icon(turnIconTurnLeft),
            maneuver(null, RIGHT) to icon(turnIconTurnRight),
            maneuver(null, STRAIGHT) to icon(turnIconTurnStraight),
            maneuver(null, UTURN) to icon(turnIconUturn, true),
            maneuver(null, SLIGHT_LEFT) to icon(turnIconTurnSlightLeft),
            maneuver(null, SLIGHT_RIGHT) to icon(turnIconTurnSlightRight),
            maneuver(null, SHARP_LEFT) to icon(turnIconTurnSharpLeft),
            maneuver(null, SHARP_RIGHT) to icon(turnIconTurnSharpRight),

            // When type != null and modifier != null
            maneuver(ARRIVE, LEFT) to icon(turnIconArriveLeft),
            maneuver(ARRIVE, RIGHT) to icon(turnIconArriveRight),
            maneuver(ARRIVE, STRAIGHT) to icon(turnIconArriveStraight),

            maneuver(DEPART, LEFT) to icon(turnIconDepartLeft),
            maneuver(DEPART, RIGHT) to icon(turnIconDepartRight),
            maneuver(DEPART, STRAIGHT) to icon(turnIconDepartStraight),

            maneuver(END_OF_ROAD, LEFT) to icon(turnIconEndRoadLeft),
            maneuver(END_OF_ROAD, RIGHT) to icon(turnIconEndRoadRight),

            maneuver(FORK, LEFT) to icon(turnIconForkLeft),
            maneuver(FORK, RIGHT) to icon(turnIconForkRight),
            maneuver(FORK, STRAIGHT) to icon(turnIconForkStraight, true),
            maneuver(FORK, SLIGHT_LEFT) to icon(turnIconForkSlightLeft),
            maneuver(FORK, SLIGHT_RIGHT) to icon(turnIconForkSlightRight),

            maneuver(MERGE, LEFT) to icon(turnIconMergeLeft),
            maneuver(MERGE, RIGHT) to icon(turnIconMergeRight),
            maneuver(MERGE, STRAIGHT) to icon(turnIconMergeStraight),
            maneuver(MERGE, SLIGHT_LEFT) to icon(turnIconMergeSlightLeft),
            maneuver(MERGE, SLIGHT_RIGHT) to icon(turnIconMergeSlightRight),

            maneuver(OFF_RAMP, LEFT) to icon(turnIconOffRampLeft),
            maneuver(OFF_RAMP, RIGHT) to icon(turnIconOffRampRight),
            maneuver(OFF_RAMP, SLIGHT_LEFT) to icon(turnIconOffRampSlightLeft),
            maneuver(OFF_RAMP, SLIGHT_RIGHT) to icon(turnIconOffRampSlightRight),

            maneuver(ON_RAMP, LEFT) to icon(turnIconOnRampLeft),
            maneuver(ON_RAMP, RIGHT) to icon(turnIconOnRampRight),
            maneuver(ON_RAMP, STRAIGHT) to icon(turnIconOnRampStraight),
            maneuver(ON_RAMP, SLIGHT_LEFT) to icon(turnIconOnRampSlightLeft),
            maneuver(ON_RAMP, SLIGHT_RIGHT) to icon(turnIconOnRampSlightRight),
            maneuver(ON_RAMP, SHARP_LEFT) to icon(turnIconOnRampSharpLeft),
            maneuver(ON_RAMP, SHARP_RIGHT) to icon(turnIconOnRampSharpRight),

            maneuver(TURN, LEFT) to icon(turnIconTurnLeft),
            maneuver(TURN, RIGHT) to icon(turnIconTurnRight),
            maneuver(TURN, UTURN) to icon(turnIconUturn, true),
            maneuver(TURN, STRAIGHT) to icon(turnIconTurnStraight),
            maneuver(TURN, SLIGHT_LEFT) to icon(turnIconTurnSlightLeft),
            maneuver(TURN, SLIGHT_RIGHT) to icon(turnIconTurnSlightRight),
            maneuver(TURN, SHARP_LEFT) to icon(turnIconTurnSharpLeft),
            maneuver(TURN, SHARP_RIGHT) to icon(turnIconTurnSharpRight),

            maneuver(ROUNDABOUT, LEFT) to icon(turnIconRoundaboutLeft, true),
            maneuver(ROUNDABOUT, RIGHT) to icon(turnIconRoundaboutRight, true),
            maneuver(ROUNDABOUT, STRAIGHT) to icon(turnIconRoundaboutStraight, true),
            maneuver(ROUNDABOUT, SHARP_LEFT) to icon(turnIconRoundaboutSharpLeft, true),
            maneuver(ROUNDABOUT, SHARP_RIGHT) to icon(turnIconRoundaboutSharpRight, true),
            maneuver(ROUNDABOUT, SLIGHT_LEFT) to icon(turnIconRoundaboutSlightLeft, true),
            maneuver(ROUNDABOUT, SLIGHT_RIGHT) to icon(turnIconRoundaboutSlightRight, true),

            maneuver(ROUNDABOUT_TURN, LEFT) to icon(turnIconRoundaboutLeft, true),
            maneuver(ROUNDABOUT_TURN, RIGHT) to icon(turnIconRoundaboutRight, true),
            maneuver(ROUNDABOUT_TURN, STRAIGHT) to icon(turnIconRoundaboutStraight, true),
            maneuver(ROUNDABOUT_TURN, SHARP_LEFT) to icon(turnIconRoundaboutSharpLeft, true),
            maneuver(ROUNDABOUT_TURN, SHARP_RIGHT) to icon(turnIconRoundaboutSharpRight, true),
            maneuver(ROUNDABOUT_TURN, SLIGHT_LEFT) to icon(turnIconRoundaboutSlightLeft, true),
            maneuver(ROUNDABOUT_TURN, SLIGHT_RIGHT) to icon(turnIconRoundaboutSlightRight, true),

            maneuver(EXIT_ROUNDABOUT, LEFT) to icon(turnIconRoundaboutLeft, true),
            maneuver(EXIT_ROUNDABOUT, RIGHT) to icon(turnIconRoundaboutRight, true),
            maneuver(EXIT_ROUNDABOUT, STRAIGHT) to icon(turnIconRoundaboutStraight, true),
            maneuver(EXIT_ROUNDABOUT, SHARP_LEFT) to icon(turnIconRoundaboutSharpLeft, true),
            maneuver(EXIT_ROUNDABOUT, SHARP_RIGHT) to icon(turnIconRoundaboutSharpRight, true),
            maneuver(EXIT_ROUNDABOUT, SLIGHT_LEFT) to icon(turnIconRoundaboutSlightLeft, true),
            maneuver(EXIT_ROUNDABOUT, SLIGHT_RIGHT) to icon(turnIconRoundaboutSlightRight, true),

            maneuver(ROTARY, LEFT) to icon(turnIconRoundaboutLeft, true),
            maneuver(ROTARY, RIGHT) to icon(turnIconRoundaboutRight, true),
            maneuver(ROTARY, STRAIGHT) to icon(turnIconRoundaboutStraight, true),
            maneuver(ROTARY, SHARP_LEFT) to icon(turnIconRoundaboutSharpLeft, true),
            maneuver(ROTARY, SHARP_RIGHT) to icon(turnIconRoundaboutSharpRight, true),
            maneuver(ROTARY, SLIGHT_LEFT) to icon(turnIconRoundaboutSlightLeft, true),
            maneuver(ROTARY, SLIGHT_RIGHT) to icon(turnIconRoundaboutSlightRight, true),

            maneuver(EXIT_ROTARY, LEFT) to icon(turnIconRoundaboutLeft, true),
            maneuver(EXIT_ROTARY, RIGHT) to icon(turnIconRoundaboutRight, true),
            maneuver(EXIT_ROTARY, STRAIGHT) to icon(turnIconRoundaboutStraight, true),
            maneuver(EXIT_ROTARY, SHARP_LEFT) to icon(turnIconRoundaboutSharpLeft, true),
            maneuver(EXIT_ROTARY, SHARP_RIGHT) to icon(turnIconRoundaboutSharpRight, true),
            maneuver(EXIT_ROTARY, SLIGHT_LEFT) to icon(turnIconRoundaboutSlightLeft, true),
            maneuver(EXIT_ROTARY, SLIGHT_RIGHT) to icon(turnIconRoundaboutSlightRight, true),
        )
    }

    fun retrieveTurnIcon(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?,
    ): ManeuverTurnIcon? {
        val iconSpec = if (isManeuverRoundabout(type)) {
            iconMap[maneuver(type, roundaboutModifier(degrees))]
        } else {
            iconMap[maneuver(type, modifier)] ?: defaultTurnIcon
        }

        return iconSpec?.let {
            ManeuverTurnIcon(
                degrees,
                drivingSide,
                if (iconSpec.drivingSideFlippable) shouldFlipIcon(drivingSide) else false,
                iconSpec.icon,
            )
        }
    }

    private fun roundaboutModifier(degrees: Float?) = degrees?.let {
        when (((degrees / ROTATION_ANGLE_45).roundToInt()) * ROTATION_ANGLE_45) {
            ROTATION_ANGLE_0, ROTATION_ANGLE_45 -> SHARP_RIGHT
            ROTATION_ANGLE_90 -> RIGHT
            ROTATION_ANGLE_135 -> SLIGHT_RIGHT
            ROTATION_ANGLE_180 -> STRAIGHT
            ROTATION_ANGLE_225 -> SLIGHT_LEFT
            ROTATION_ANGLE_270 -> LEFT
            ROTATION_ANGLE_315, ROTATION_ANGLE_360 -> SHARP_LEFT
            else -> null
        }
    }

    private fun shouldFlipIcon(drivingSide: String?): Boolean = when {
        !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_LEFT -> true
        !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_RIGHT -> false
        else -> false
    }

    private fun isManeuverRoundabout(type: String?): Boolean = when {
        !type.isNullOrEmpty() &&
            (
                type == ROUNDABOUT ||
                    type == ROUNDABOUT_TURN ||
                    type == EXIT_ROUNDABOUT ||
                    type == ROTARY ||
                    type == EXIT_ROTARY
                ) -> {
            true
        }
        else -> false
    }

    private fun maneuver(type: String?, modifier: String?) =
        ManeuverTypeModifierPair(type, modifier)

    private fun icon(icon: Int, drivingSideFlippable: Boolean = false) =
        IconSpec(icon, drivingSideFlippable)

    private companion object {
        private const val DRIVING_SIDE_LEFT = "left"
        private const val DRIVING_SIDE_RIGHT = "right"
        private const val ROTATION_ANGLE_0 = 0f
        private const val ROTATION_ANGLE_45 = 45f
        private const val ROTATION_ANGLE_90 = 90f
        private const val ROTATION_ANGLE_135 = 135f
        private const val ROTATION_ANGLE_180 = 180f
        private const val ROTATION_ANGLE_225 = 225f
        private const val ROTATION_ANGLE_270 = 270f
        private const val ROTATION_ANGLE_315 = 315f
        private const val ROTATION_ANGLE_360 = 360f
    }

    private data class IconSpec(val icon: Int, val drivingSideFlippable: Boolean)
}
