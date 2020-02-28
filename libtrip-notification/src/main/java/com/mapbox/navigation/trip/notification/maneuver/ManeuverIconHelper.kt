package com.mapbox.navigation.trip.notification.maneuver

import android.graphics.Canvas
import android.graphics.PointF
import androidx.core.util.Pair

// Step Maneuver Types
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

object ManeuverIconHelper {

    const val DEFAULT_ROUNDABOUT_ANGLE = 180f

    private const val TOP_ROUNDABOUT_ANGLE_LIMIT = 300f
    private const val BOTTOM_ROUNDABOUT_ANGLE_LIMIT = 60f

    @JvmField
    val MANEUVER_ICON_DRAWER_MAP: Map<Pair<String, String>, ManeuverIconDrawer> =
        object : HashMap<Pair<String, String>, ManeuverIconDrawer>() {
            init {
                put(Pair(STEP_MANEUVER_TYPE_MERGE, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawMerge(canvas, primaryColor, secondaryColor, size)
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_OFF_RAMP, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawOffRamp(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_FORK, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawFork(canvas, primaryColor, secondaryColor, size)
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROUNDABOUT, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawRoundabout(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size,
                                roundaboutAngle
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawRoundabout(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size,
                                roundaboutAngle
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawRoundabout(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size,
                                roundaboutAngle
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROTARY, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawRoundabout(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size,
                                roundaboutAngle
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_EXIT_ROTARY, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawRoundabout(
                                canvas,
                                primaryColor,
                                secondaryColor,
                                size,
                                roundaboutAngle
                            )
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrive(canvas, primaryColor, size)
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_STRAIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrive(canvas, primaryColor, size)
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_RIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_LEFT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_RIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SHARP_RIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SLIGHT_LEFT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_LEFT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SHARP_LEFT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_UTURN),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrow180Right(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_STRAIGHT),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size)
                        }
                    })
                put(Pair(null, null),
                    object : ManeuverIconDrawer {
                        override fun drawManeuverIcon(
                            canvas: Canvas,
                            primaryColor: Int,
                            secondaryColor: Int,
                            size: PointF,
                            roundaboutAngle: Float
                        ) {
                            ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size)
                        }
                    })
            }
        }

    @JvmField
    val SHOULD_FLIP_MODIFIERS: Set<String> = object : HashSet<String>() {
        init {
            add(STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
            add(STEP_MANEUVER_MODIFIER_LEFT)
            add(STEP_MANEUVER_MODIFIER_SHARP_LEFT)
            add(STEP_MANEUVER_MODIFIER_UTURN)
        }
    }

    @JvmField
    val ROUNDABOUT_MANEUVER_TYPES: Set<String> = object : HashSet<String>() {
        init {
            add(STEP_MANEUVER_TYPE_ROTARY)
            add(STEP_MANEUVER_TYPE_ROUNDABOUT)
            add(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN)
            add(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT)
            add(STEP_MANEUVER_TYPE_EXIT_ROTARY)
        }
    }

    @JvmField
    val MANEUVER_TYPES_WITH_NULL_MODIFIERS: Set<String> = object : HashSet<String>() {
        init {
            add(STEP_MANEUVER_TYPE_OFF_RAMP)
            add(STEP_MANEUVER_TYPE_FORK)
            add(STEP_MANEUVER_TYPE_ROUNDABOUT)
            add(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN)
            add(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT)
            add(STEP_MANEUVER_TYPE_ROTARY)
            add(STEP_MANEUVER_TYPE_EXIT_ROTARY)
        }
    }

    @JvmStatic
    fun isManeuverIconNeedFlip(
        maneuverType: String?,
        maneuverModifier: String?,
        drivingSide: String?
    ): Boolean {
        val leftDriving = STEP_MANEUVER_MODIFIER_LEFT == drivingSide
        val roundaboutManeuverType = ROUNDABOUT_MANEUVER_TYPES.contains(maneuverType)
        val uturnManeuverModifier =
            !maneuverModifier.isNullOrBlank() && STEP_MANEUVER_MODIFIER_UTURN.contains(
                maneuverModifier
            )

        var flip = SHOULD_FLIP_MODIFIERS.contains(maneuverModifier)
        if (roundaboutManeuverType) {
            flip = leftDriving
        }

        return if (leftDriving && uturnManeuverModifier) {
            !flip
        } else {
            flip
        }
    }

    @JvmStatic
    fun adjustRoundaboutAngle(roundaboutAngle: Float): Float =
        when {
            roundaboutAngle < BOTTOM_ROUNDABOUT_ANGLE_LIMIT -> BOTTOM_ROUNDABOUT_ANGLE_LIMIT
            roundaboutAngle > TOP_ROUNDABOUT_ANGLE_LIMIT -> TOP_ROUNDABOUT_ANGLE_LIMIT
            else -> roundaboutAngle
        }
}
