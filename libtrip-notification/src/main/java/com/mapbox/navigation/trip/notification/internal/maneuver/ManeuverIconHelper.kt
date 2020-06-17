package com.mapbox.navigation.trip.notification.internal.maneuver

import android.graphics.Canvas
import android.graphics.PointF
import androidx.core.util.Pair
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver

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
                put(
                    Pair(StepManeuver.MERGE, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.OFF_RAMP, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.FORK, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.ROUNDABOUT, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.ROUNDABOUT_TURN, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.EXIT_ROUNDABOUT, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.ROTARY, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.EXIT_ROTARY, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.ARRIVE, null),
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
                    }
                )
                put(
                    Pair(StepManeuver.ARRIVE, ManeuverModifier.STRAIGHT),
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
                    }
                )
                put(
                    Pair(StepManeuver.ARRIVE, ManeuverModifier.RIGHT),
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
                    }
                )
                put(
                    Pair(StepManeuver.ARRIVE, ManeuverModifier.LEFT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.SLIGHT_RIGHT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.RIGHT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.SHARP_RIGHT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.SLIGHT_LEFT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.LEFT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.SHARP_LEFT),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.UTURN),
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
                    }
                )
                put(
                    Pair(null, ManeuverModifier.STRAIGHT),
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
                    }
                )
                put(
                    Pair(null, null),
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
                    }
                )
            }
        }

    /**
     * Set of modifiers that should be flipped
     */
    @JvmField
    val SHOULD_FLIP_MODIFIERS: Set<String> = object : HashSet<String>() {
        init {
            add(ManeuverModifier.SLIGHT_LEFT)
            add(ManeuverModifier.LEFT)
            add(ManeuverModifier.SHARP_LEFT)
            add(ManeuverModifier.UTURN)
        }
    }

    /**
     * Set of roundabout modifiers
     */
    @JvmField
    val ROUNDABOUT_MANEUVER_TYPES: Set<String> = object : HashSet<String>() {
        init {
            add(StepManeuver.ROTARY)
            add(StepManeuver.ROUNDABOUT)
            add(StepManeuver.ROUNDABOUT_TURN)
            add(StepManeuver.EXIT_ROUNDABOUT)
            add(StepManeuver.EXIT_ROTARY)
        }
    }

    /**
     * Set of maneuver types without modifiers
     */
    @JvmField
    val MANEUVER_TYPES_WITH_NULL_MODIFIERS: Set<String> = object : HashSet<String>() {
        init {
            add(StepManeuver.OFF_RAMP)
            add(StepManeuver.FORK)
            add(StepManeuver.ROUNDABOUT)
            add(StepManeuver.ROUNDABOUT_TURN)
            add(StepManeuver.EXIT_ROUNDABOUT)
            add(StepManeuver.ROTARY)
            add(StepManeuver.EXIT_ROTARY)
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
        val leftDriving = ManeuverModifier.LEFT == drivingSide
        val roundaboutManeuverType = ROUNDABOUT_MANEUVER_TYPES.contains(maneuverType)
        val uturnManeuverModifier =
            !maneuverModifier.isNullOrBlank() && ManeuverModifier.UTURN.contains(
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
