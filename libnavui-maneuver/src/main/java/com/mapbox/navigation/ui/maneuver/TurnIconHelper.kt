package com.mapbox.navigation.ui.maneuver

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
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.ManeuverTypeModifierPair
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlin.math.roundToInt

internal class TurnIconHelper(
    turnIconResources: TurnIconResources
) {

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

    // ManeuverTypeModifierPair<Type, Modifier>
    private val turnIconMap: Map<ManeuverTypeModifierPair, Int> = mapOf(
        // When type == null and modifier == null
        ManeuverTypeModifierPair(null, null) to turnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair("", "") to turnIconResources.turnIconTurnStraight,

        // When type != null and modifier == null
        ManeuverTypeModifierPair(ARRIVE, null) to turnIconResources.turnIconArrive,
        ManeuverTypeModifierPair(DEPART, null) to turnIconResources.turnIconDepart,
        ManeuverTypeModifierPair(ON_RAMP, null) to turnIconResources.turnIconOnRamp,
        ManeuverTypeModifierPair(OFF_RAMP, null) to turnIconResources.turnIconOffRamp,
        ManeuverTypeModifierPair(FORK, null) to turnIconResources.turnIconFork,
        ManeuverTypeModifierPair(TURN, null) to turnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(MERGE, null) to turnIconResources.turnIconMergeStraight,
        ManeuverTypeModifierPair(END_OF_ROAD, null) to
            turnIconResources.turnIconEndRoadLeft,

        // When type = null and modifier != null
        ManeuverTypeModifierPair(null, LEFT) to turnIconResources.turnIconTurnLeft,
        ManeuverTypeModifierPair(null, RIGHT) to turnIconResources.turnIconTurnRight,
        ManeuverTypeModifierPair(null, STRAIGHT) to turnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(null, UTURN) to turnIconResources.turnIconUturn,
        ManeuverTypeModifierPair(null, SLIGHT_LEFT) to
            turnIconResources.turnIconTurnSlightLeft,
        ManeuverTypeModifierPair(null, SLIGHT_RIGHT) to
            turnIconResources.turnIconTurnSlightRight,
        ManeuverTypeModifierPair(null, SHARP_LEFT) to turnIconResources.turnIconTurnSharpLeft,
        ManeuverTypeModifierPair(null, SHARP_RIGHT) to
            turnIconResources.turnIconTurnSharpRight,

        // When type != null and modifier != null
        ManeuverTypeModifierPair(ARRIVE, LEFT) to turnIconResources.turnIconArriveLeft,
        ManeuverTypeModifierPair(ARRIVE, RIGHT) to turnIconResources.turnIconArriveRight,
        ManeuverTypeModifierPair(ARRIVE, STRAIGHT) to turnIconResources.turnIconArriveStraight,
        ManeuverTypeModifierPair(DEPART, LEFT) to turnIconResources.turnIconDepartLeft,
        ManeuverTypeModifierPair(DEPART, RIGHT) to turnIconResources.turnIconDepartRight,
        ManeuverTypeModifierPair(DEPART, STRAIGHT) to turnIconResources.turnIconDepartStraight,
        ManeuverTypeModifierPair(END_OF_ROAD, LEFT) to turnIconResources.turnIconEndRoadLeft,
        ManeuverTypeModifierPair(END_OF_ROAD, RIGHT) to turnIconResources.turnIconEndRoadRight,
        ManeuverTypeModifierPair(FORK, LEFT) to turnIconResources.turnIconForkLeft,
        ManeuverTypeModifierPair(FORK, RIGHT) to turnIconResources.turnIconForkRight,
        ManeuverTypeModifierPair(FORK, STRAIGHT) to turnIconResources.turnIconForkStraight,
        ManeuverTypeModifierPair(FORK, SLIGHT_LEFT) to turnIconResources.turnIconForkSlightLeft,
        ManeuverTypeModifierPair(FORK, SLIGHT_RIGHT) to turnIconResources.turnIconForkSlightRight,
        ManeuverTypeModifierPair(MERGE, LEFT) to turnIconResources.turnIconMergeLeft,
        ManeuverTypeModifierPair(MERGE, RIGHT) to turnIconResources.turnIconMergeRight,
        ManeuverTypeModifierPair(MERGE, STRAIGHT) to turnIconResources.turnIconMergeStraight,
        ManeuverTypeModifierPair(MERGE, SLIGHT_LEFT) to turnIconResources.turnIconMergeSlightLeft,
        ManeuverTypeModifierPair(MERGE, SLIGHT_RIGHT) to turnIconResources.turnIconMergeSlightRight,
        ManeuverTypeModifierPair(OFF_RAMP, LEFT) to turnIconResources.turnIconOffRampLeft,
        ManeuverTypeModifierPair(OFF_RAMP, RIGHT) to turnIconResources.turnIconOffRampRight,
        ManeuverTypeModifierPair(OFF_RAMP, SLIGHT_LEFT) to
            turnIconResources.turnIconOffRampSlightLeft,
        ManeuverTypeModifierPair(OFF_RAMP, SLIGHT_RIGHT) to
            turnIconResources.turnIconOffRampSlightRight,
        ManeuverTypeModifierPair(ON_RAMP, LEFT) to turnIconResources.turnIconOnRampLeft,
        ManeuverTypeModifierPair(ON_RAMP, RIGHT) to turnIconResources.turnIconOnRampRight,
        ManeuverTypeModifierPair(ON_RAMP, STRAIGHT) to turnIconResources.turnIconOnRampStraight,
        ManeuverTypeModifierPair(ON_RAMP, SLIGHT_LEFT) to
            turnIconResources.turnIconOnRampSlightLeft,
        ManeuverTypeModifierPair(ON_RAMP, SLIGHT_RIGHT) to
            turnIconResources.turnIconOnRampSlightRight,
        ManeuverTypeModifierPair(ON_RAMP, SHARP_LEFT) to turnIconResources.turnIconOnRampSharpLeft,
        ManeuverTypeModifierPair(ON_RAMP, SHARP_RIGHT) to
            turnIconResources.turnIconOnRampSharpRight,
        ManeuverTypeModifierPair(TURN, LEFT) to turnIconResources.turnIconTurnLeft,
        ManeuverTypeModifierPair(TURN, RIGHT) to turnIconResources.turnIconTurnRight,
        ManeuverTypeModifierPair(TURN, UTURN) to turnIconResources.turnIconUturn,
        ManeuverTypeModifierPair(TURN, STRAIGHT) to turnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(TURN, SLIGHT_LEFT) to turnIconResources.turnIconTurnSlightLeft,
        ManeuverTypeModifierPair(TURN, SLIGHT_RIGHT) to turnIconResources.turnIconTurnSlightRight,
        ManeuverTypeModifierPair(TURN, SHARP_LEFT) to turnIconResources.turnIconTurnSharpLeft,
        ManeuverTypeModifierPair(TURN, SHARP_RIGHT) to turnIconResources.turnIconTurnSharpRight
    )

    private val roundaboutIconMap: Map<ManeuverTypeModifierPair, Int> = mapOf(
        ManeuverTypeModifierPair(null, null) to turnIconResources.turnIconRoundabout,
        ManeuverTypeModifierPair(ROUNDABOUT, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(ROUNDABOUT, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(ROUNDABOUT, STRAIGHT) to
            turnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(ROUNDABOUT, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(ROUNDABOUT, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(ROUNDABOUT, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(ROUNDABOUT, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, RIGHT) to
            turnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, STRAIGHT) to
            turnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(ROUNDABOUT_TURN, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, RIGHT) to
            turnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, STRAIGHT) to
            turnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(EXIT_ROUNDABOUT, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(ROTARY, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(ROTARY, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(ROTARY, STRAIGHT) to turnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(ROTARY, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(ROTARY, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(ROTARY, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(ROTARY, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(EXIT_ROTARY, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(EXIT_ROTARY, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(EXIT_ROTARY, STRAIGHT) to
            turnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(EXIT_ROTARY, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(EXIT_ROTARY, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(EXIT_ROTARY, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(EXIT_ROTARY, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight
    )

    fun retrieveTurnIcon(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?,
    ): ManeuverTurnIcon? {
        val isRoundabout = isManeuverRoundabout(type)
        return when {
            isRoundabout -> {
                generateRoundaboutIcons(type, degrees, drivingSide)
            }
            else -> {
                generateTurnIcons(type, degrees, modifier, drivingSide)
            }
        }
    }

    private fun generateRoundaboutIcons(
        type: String?,
        degrees: Float?,
        drivingSide: String?,
    ): ManeuverTurnIcon {
        return ifNonNull(degrees) { rotateBy ->
            val angleAfterRounding =
                ((rotateBy / ROTATION_ANGLE_45).roundToInt()) * ROTATION_ANGLE_45
            val shouldFlip = shouldFlipIcon(drivingSide)
            when (angleAfterRounding) {
                ROTATION_ANGLE_0, ROTATION_ANGLE_45 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, SHARP_RIGHT)]
                    )
                }
                ROTATION_ANGLE_90 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, RIGHT)]
                    )
                }
                ROTATION_ANGLE_135 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, SLIGHT_RIGHT)]
                    )
                }
                ROTATION_ANGLE_180 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, STRAIGHT)]
                    )
                }
                ROTATION_ANGLE_225 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, SLIGHT_LEFT)]
                    )
                }
                ROTATION_ANGLE_270 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, LEFT)]
                    )
                }
                ROTATION_ANGLE_315, ROTATION_ANGLE_360 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, SHARP_LEFT)]
                    )
                }
                else -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(null, null)]
                    )
                }
            }
        } ?: ManeuverTurnIcon(
            degrees,
            drivingSide,
            shouldFlipIcon(drivingSide),
            roundaboutIconMap[ManeuverTypeModifierPair(null, null)]
        )
    }

    private fun generateTurnIcons(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?
    ): ManeuverTurnIcon? {
        return when {
            type.isNullOrEmpty() && modifier.isNullOrEmpty() -> {
                ManeuverTurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    turnIconMap[ManeuverTypeModifierPair(null, null)]
                )
            }
            type.isNullOrEmpty() && !modifier.isNullOrEmpty() -> {
                val shouldFlip = shouldFlipUturn(modifier, drivingSide)
                ManeuverTurnIcon(
                    degrees,
                    drivingSide,
                    shouldFlip,
                    getTurnIconWithNullType(modifier)
                )
            }
            !type.isNullOrEmpty() && modifier.isNullOrEmpty() -> {
                ManeuverTurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    getTurnIconWithNullModifier(type)
                )
            }
            !type.isNullOrEmpty() && !modifier.isNullOrEmpty() -> {
                val shouldFlip = shouldFlipUturn(modifier, drivingSide)
                ManeuverTurnIcon(
                    degrees,
                    drivingSide,
                    shouldFlip,
                    getTurnIconWithTypeAndModifier(type, modifier)
                )
            }
            else -> null
        }
    }

    private fun getTurnIconWithNullType(modifier: String): Int? {
        return when (modifier) {
            LEFT, RIGHT, STRAIGHT, UTURN, SLIGHT_RIGHT, SLIGHT_LEFT, SHARP_RIGHT, SHARP_LEFT -> {
                turnIconMap[ManeuverTypeModifierPair(null, modifier)]
            }
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithNullModifier(type: String): Int? {
        return when (type) {
            ARRIVE, DEPART, ON_RAMP, OFF_RAMP, FORK, TURN, MERGE, END_OF_ROAD -> {
                turnIconMap[ManeuverTypeModifierPair(type, null)]
            }
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithTypeAndModifier(type: String, modifier: String): Int? {
        return when {
            type == ARRIVE && modifier == LEFT ||
                type == ARRIVE && modifier == RIGHT ||
                type == ARRIVE && modifier == STRAIGHT ||
                type == DEPART && modifier == LEFT ||
                type == DEPART && modifier == RIGHT ||
                type == DEPART && modifier == STRAIGHT ||
                type == END_OF_ROAD && modifier == LEFT ||
                type == END_OF_ROAD && modifier == RIGHT ||
                type == FORK && modifier == LEFT ||
                type == FORK && modifier == RIGHT ||
                type == FORK && modifier == STRAIGHT ||
                type == FORK && modifier == SLIGHT_LEFT ||
                type == FORK && modifier == SLIGHT_RIGHT ||
                type == MERGE && modifier == LEFT ||
                type == MERGE && modifier == RIGHT ||
                type == MERGE && modifier == STRAIGHT ||
                type == MERGE && modifier == SLIGHT_LEFT ||
                type == MERGE && modifier == SLIGHT_RIGHT ||
                type == OFF_RAMP && modifier == LEFT ||
                type == OFF_RAMP && modifier == RIGHT ||
                type == OFF_RAMP && modifier == SLIGHT_LEFT ||
                type == OFF_RAMP && modifier == SLIGHT_RIGHT ||
                type == ON_RAMP && modifier == LEFT ||
                type == ON_RAMP && modifier == RIGHT ||
                type == ON_RAMP && modifier == STRAIGHT ||
                type == ON_RAMP && modifier == SLIGHT_LEFT ||
                type == ON_RAMP && modifier == SLIGHT_RIGHT ||
                type == ON_RAMP && modifier == SHARP_LEFT ||
                type == ON_RAMP && modifier == SHARP_RIGHT ||
                type == TURN && modifier == LEFT ||
                type == TURN && modifier == RIGHT ||
                type == TURN && modifier == UTURN ||
                type == TURN && modifier == STRAIGHT ||
                type == TURN && modifier == SLIGHT_LEFT ||
                type == TURN && modifier == SLIGHT_RIGHT ||
                type == TURN && modifier == SHARP_LEFT ||
                type == TURN && modifier == SHARP_RIGHT
            -> turnIconMap[ManeuverTypeModifierPair(type, modifier)]
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun shouldFlipUturn(modifier: String?, drivingSide: String?): Boolean {
        return if (modifier == UTURN) {
            shouldFlipIcon(drivingSide)
        } else {
            false
        }
    }

    private fun shouldFlipIcon(drivingSide: String?): Boolean {
        return when {
            !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_LEFT -> true
            !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_RIGHT -> false
            else -> false
        }
    }

    private fun isManeuverRoundabout(type: String?): Boolean {
        return when {
            !type.isNullOrEmpty()
                && (
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
    }
}
