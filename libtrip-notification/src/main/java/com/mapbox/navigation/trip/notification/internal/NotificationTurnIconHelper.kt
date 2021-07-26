package com.mapbox.navigation.trip.notification.internal

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.ManeuverTypeModifierPair
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlin.math.roundToInt

internal class NotificationTurnIconHelper(
    notificationTurnIconResources: NotificationTurnIconResources
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
        ManeuverTypeModifierPair(
            null, null
        ) to notificationTurnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(
            "", ""
        ) to notificationTurnIconResources.turnIconTurnStraight,

        // When type != null and modifier == null
        ManeuverTypeModifierPair(StepManeuver.ARRIVE, null) to
            notificationTurnIconResources.turnIconArrive,
        ManeuverTypeModifierPair(StepManeuver.DEPART, null) to
            notificationTurnIconResources.turnIconDepart,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, null) to
            notificationTurnIconResources.turnIconOnRamp,
        ManeuverTypeModifierPair(StepManeuver.OFF_RAMP, null) to
            notificationTurnIconResources.turnIconOffRamp,
        ManeuverTypeModifierPair(StepManeuver.FORK, null) to
            notificationTurnIconResources.turnIconFork,
        ManeuverTypeModifierPair(StepManeuver.TURN, null) to
            notificationTurnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(StepManeuver.MERGE, null) to
            notificationTurnIconResources.turnIconMergeStraight,
        ManeuverTypeModifierPair(StepManeuver.END_OF_ROAD, null) to
            notificationTurnIconResources.turnIconEndRoadLeft,

        // When type = null and modifier != null
        ManeuverTypeModifierPair(null, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconTurnLeft,
        ManeuverTypeModifierPair(null, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconTurnRight,
        ManeuverTypeModifierPair(null, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(null, ManeuverModifier.UTURN) to
            notificationTurnIconResources.turnIconUturn,
        ManeuverTypeModifierPair(null, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconTurnSlightLeft,
        ManeuverTypeModifierPair(null, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconTurnSlightRight,
        ManeuverTypeModifierPair(null, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconTurnSharpLeft,
        ManeuverTypeModifierPair(null, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconTurnSharpRight,

        // When type != null and modifier != null
        ManeuverTypeModifierPair(StepManeuver.ARRIVE, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconArriveLeft,
        ManeuverTypeModifierPair(StepManeuver.ARRIVE, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconArriveRight,
        ManeuverTypeModifierPair(StepManeuver.ARRIVE, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconArriveStraight,
        ManeuverTypeModifierPair(StepManeuver.DEPART, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconDepartLeft,
        ManeuverTypeModifierPair(StepManeuver.DEPART, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconDepartRight,
        ManeuverTypeModifierPair(StepManeuver.DEPART, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconDepartStraight,
        ManeuverTypeModifierPair(StepManeuver.END_OF_ROAD, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconEndRoadLeft,
        ManeuverTypeModifierPair(StepManeuver.END_OF_ROAD, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconEndRoadRight,
        ManeuverTypeModifierPair(StepManeuver.FORK, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconForkLeft,
        ManeuverTypeModifierPair(StepManeuver.FORK, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconForkRight,
        ManeuverTypeModifierPair(StepManeuver.FORK, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconForkStraight,
        ManeuverTypeModifierPair(StepManeuver.FORK, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconForkSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.FORK, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconForkSlightRight,
        ManeuverTypeModifierPair(StepManeuver.MERGE, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconMergeLeft,
        ManeuverTypeModifierPair(StepManeuver.MERGE, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconMergeRight,
        ManeuverTypeModifierPair(StepManeuver.MERGE, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconMergeStraight,
        ManeuverTypeModifierPair(StepManeuver.MERGE, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconMergeSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.MERGE, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconMergeSlightRight,
        ManeuverTypeModifierPair(StepManeuver.OFF_RAMP, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconOffRampLeft,
        ManeuverTypeModifierPair(StepManeuver.OFF_RAMP, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconOffRampRight,
        ManeuverTypeModifierPair(StepManeuver.OFF_RAMP, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconOffRampSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.OFF_RAMP, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconOffRampSlightRight,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconOnRampLeft,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconOnRampRight,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconOnRampStraight,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconOnRampSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconOnRampSlightRight,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconOnRampSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.ON_RAMP, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconOnRampSharpRight,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconTurnLeft,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconTurnRight,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.UTURN) to
            notificationTurnIconResources.turnIconUturn,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconTurnStraight,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconTurnSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconTurnSlightRight,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconTurnSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.TURN, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconTurnSharpRight
    )

    private val roundaboutIconMap: Map<ManeuverTypeModifierPair, Int> = mapOf(
        ManeuverTypeModifierPair(null, null) to
            notificationTurnIconResources.turnIconRoundabout,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.ROUNDABOUT_TURN, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROUNDABOUT, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.ROTARY, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSlightRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.LEFT) to
            notificationTurnIconResources.turnIconRoundaboutLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.STRAIGHT) to
            notificationTurnIconResources.turnIconRoundaboutStraight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.SHARP_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSharpLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.SHARP_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSharpRight,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.SLIGHT_LEFT) to
            notificationTurnIconResources.turnIconRoundaboutSlightLeft,
        ManeuverTypeModifierPair(StepManeuver.EXIT_ROTARY, ManeuverModifier.SLIGHT_RIGHT) to
            notificationTurnIconResources.turnIconRoundaboutSlightRight
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
                        roundaboutIconMap[
                            ManeuverTypeModifierPair(type, ManeuverModifier.SHARP_RIGHT)
                        ]
                    )
                }
                ROTATION_ANGLE_90 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, ManeuverModifier.RIGHT)]
                    )
                }
                ROTATION_ANGLE_135 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[
                            ManeuverTypeModifierPair(type, ManeuverModifier.SLIGHT_RIGHT)
                        ]
                    )
                }
                ROTATION_ANGLE_180 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, ManeuverModifier.STRAIGHT)]
                    )
                }
                ROTATION_ANGLE_225 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[
                            ManeuverTypeModifierPair(type, ManeuverModifier.SLIGHT_LEFT)
                        ]
                    )
                }
                ROTATION_ANGLE_270 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[ManeuverTypeModifierPair(type, ManeuverModifier.LEFT)]
                    )
                }
                ROTATION_ANGLE_315, ROTATION_ANGLE_360 -> {
                    ManeuverTurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[
                            ManeuverTypeModifierPair(type, ManeuverModifier.SHARP_LEFT)
                        ]
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
            ManeuverModifier.LEFT, ManeuverModifier.RIGHT, ManeuverModifier.STRAIGHT,
            ManeuverModifier.UTURN, ManeuverModifier.SLIGHT_RIGHT, ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_RIGHT, ManeuverModifier.SHARP_LEFT -> {
                turnIconMap[ManeuverTypeModifierPair(null, modifier)]
            }
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithNullModifier(type: String): Int? {
        return when (type) {
            StepManeuver.ARRIVE, StepManeuver.DEPART, StepManeuver.ON_RAMP,
            StepManeuver.OFF_RAMP, StepManeuver.FORK, StepManeuver.TURN,
            StepManeuver.MERGE, StepManeuver.END_OF_ROAD -> {
                turnIconMap[ManeuverTypeModifierPair(type, null)]
            }
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithTypeAndModifier(type: String, modifier: String): Int? {
        return when {
            type == StepManeuver.ARRIVE && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.ARRIVE && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.ARRIVE && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.DEPART && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.DEPART && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.DEPART && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.END_OF_ROAD && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.END_OF_ROAD && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.FORK && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.FORK && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.FORK && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.FORK && modifier == ManeuverModifier.SLIGHT_LEFT ||
                type == StepManeuver.FORK && modifier == ManeuverModifier.SLIGHT_RIGHT ||
                type == StepManeuver.MERGE && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.MERGE && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.MERGE && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.MERGE && modifier == ManeuverModifier.SLIGHT_LEFT ||
                type == StepManeuver.MERGE && modifier == ManeuverModifier.SLIGHT_RIGHT ||
                type == StepManeuver.OFF_RAMP && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.OFF_RAMP && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.OFF_RAMP && modifier == ManeuverModifier.SLIGHT_LEFT ||
                type == StepManeuver.OFF_RAMP && modifier == ManeuverModifier.SLIGHT_RIGHT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.SLIGHT_LEFT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.SLIGHT_RIGHT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.SHARP_LEFT ||
                type == StepManeuver.ON_RAMP && modifier == ManeuverModifier.SHARP_RIGHT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.LEFT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.RIGHT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.UTURN ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.STRAIGHT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.SLIGHT_LEFT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.SLIGHT_RIGHT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.SHARP_LEFT ||
                type == StepManeuver.TURN && modifier == ManeuverModifier.SHARP_RIGHT
            -> turnIconMap[ManeuverTypeModifierPair(type, modifier)]
            else -> {
                turnIconMap[ManeuverTypeModifierPair(null, null)]
            }
        }
    }

    private fun shouldFlipUturn(modifier: String?, drivingSide: String?): Boolean {
        return if (modifier == ManeuverModifier.UTURN) {
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
                    type == StepManeuver.ROUNDABOUT ||
                        type == StepManeuver.ROUNDABOUT_TURN ||
                        type == StepManeuver.EXIT_ROUNDABOUT ||
                        type == StepManeuver.ROTARY ||
                        type == StepManeuver.EXIT_ROTARY
                    ) -> {
                true
            }
            else -> false
        }
    }
}
