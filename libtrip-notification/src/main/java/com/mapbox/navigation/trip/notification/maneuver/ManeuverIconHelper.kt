package com.mapbox.navigation.trip.notification.maneuver

import android.graphics.Canvas
import android.graphics.PointF
import androidx.core.util.Pair

// Step Maneuver Types
/**
 * Step maneuver type *turn*
 */
const val STEP_MANEUVER_TYPE_TURN = "turn"
/**
 * Step maneuver type *new name*
 */
const val STEP_MANEUVER_TYPE_NEW_NAME = "new name"
/**
 * Step maneuver type *depart*
 */
const val STEP_MANEUVER_TYPE_DEPART = "depart"
/**
 * Step maneuver type *arrive*
 */
const val STEP_MANEUVER_TYPE_ARRIVE = "arrive"
/**
 * Step maneuver type *merge*
 */
const val STEP_MANEUVER_TYPE_MERGE = "merge"
/**
 * Step maneuver type *on ramp*
 */
const val STEP_MANEUVER_TYPE_ON_RAMP = "on ramp"
/**
 * Step maneuver type *off ramp*
 */
const val STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp"
/**
 * Step maneuver type *fork*
 */
const val STEP_MANEUVER_TYPE_FORK = "fork"
/**
 * Step maneuver type *end of road*
 */
const val STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road"
/**
 * Step maneuver type *continue*
 */
const val STEP_MANEUVER_TYPE_CONTINUE = "continue"
/**
 * Step maneuver type *roundabout*
 */
const val STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout"
/**
 * Step maneuver type *rotary*
 */
const val STEP_MANEUVER_TYPE_ROTARY = "rotary"
/**
 * Step maneuver type *roundabout turn*
 */
const val STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn"
/**
 * Step maneuver type *notification*
 */
const val STEP_MANEUVER_TYPE_NOTIFICATION = "notification"
/**
 * Step maneuver type *exit roundabout*
 */
const val STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout"
/**
 * Step maneuver type *exit rotary*
 */
const val STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary"

// Step Maneuver Modifiers
/**
 * Step maneuver modifier *u turn*
 */
const val STEP_MANEUVER_MODIFIER_UTURN = "uturn"
/**
 * Step maneuver modifier *sharp right*
 */
const val STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right"
/**
 * Step maneuver modifier *right*
 */
const val STEP_MANEUVER_MODIFIER_RIGHT = "right"
/**
 * Step maneuver modifier *slight right*
 */
const val STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right"
/**
 * Step maneuver modifier *straight*
 */
const val STEP_MANEUVER_MODIFIER_STRAIGHT = "straight"
/**
 * Step maneuver modifier *slight left*
 */
const val STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left"
/**
 * Step maneuver modifier *left*
 */
const val STEP_MANEUVER_MODIFIER_LEFT = "left"
/**
 * Step maneuver modifier *sharp left*
 */
const val STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left"

/**
 * A helping class for creating maneuvers that are draw on the [Canvas]
 */
object ManeuverIconHelper {

    /**
     * Default *roundabout* angle
     */
    const val DEFAULT_ROUNDABOUT_ANGLE = 180f

    private const val TOP_ROUNDABOUT_ANGLE_LIMIT = 300f
    private const val BOTTOM_ROUNDABOUT_ANGLE_LIMIT = 60f

    /**
     * Map of the maneuver type+modifier and the maneuver's [ManeuverIconDrawer]
     */
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

    /**
     * Set of modifiers that should be flipped
     */
    @JvmField
    val SHOULD_FLIP_MODIFIERS: Set<String> = object : HashSet<String>() {
        init {
            add(STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
            add(STEP_MANEUVER_MODIFIER_LEFT)
            add(STEP_MANEUVER_MODIFIER_SHARP_LEFT)
            add(STEP_MANEUVER_MODIFIER_UTURN)
        }
    }

    /**
     * Set of roundabout modifiers
     */
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

    /**
     * Set of maneuver types without modifiers
     */
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

    /**
     * Returns whether the maneuver's icon should be flipped
     */
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

    /**
     * Provides the roundabout's angle according to angle limits
     */
    @JvmStatic
    fun adjustRoundaboutAngle(roundaboutAngle: Float): Float =
        when {
            roundaboutAngle < BOTTOM_ROUNDABOUT_ANGLE_LIMIT -> BOTTOM_ROUNDABOUT_ANGLE_LIMIT
            roundaboutAngle > TOP_ROUNDABOUT_ANGLE_LIMIT -> TOP_ROUNDABOUT_ANGLE_LIMIT
            else -> roundaboutAngle
        }
}
