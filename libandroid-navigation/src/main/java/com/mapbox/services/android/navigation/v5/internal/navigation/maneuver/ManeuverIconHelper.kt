package com.mapbox.services.android.navigation.v5.internal.navigation.maneuver

import androidx.core.util.Pair
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROTARY
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_MERGE
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN

object ManeuverIconHelper {

    const val DEFAULT_ROUNDABOUT_ANGLE = 180f

    private const val TOP_ROUNDABOUT_ANGLE_LIMIT = 300f
    private const val BOTTOM_ROUNDABOUT_ANGLE_LIMIT = 60f

    @JvmField
    val MANEUVER_ICON_DRAWER_MAP: Map<Pair<String, String>, ManeuverIconDrawer> =
        object : HashMap<Pair<String, String>, ManeuverIconDrawer>() {
            init {
                put(Pair(STEP_MANEUVER_TYPE_MERGE, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawMerge(canvas, primaryColor, secondaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_OFF_RAMP, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawOffRamp(canvas, primaryColor, secondaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_FORK, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawFork(canvas, primaryColor, secondaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROUNDABOUT, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawRoundabout(
                            canvas,
                            primaryColor,
                            secondaryColor,
                            size,
                            roundaboutAngle
                        )
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawRoundabout(
                            canvas,
                            primaryColor,
                            secondaryColor,
                            size,
                            roundaboutAngle
                        )
                    })
                put(Pair(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawRoundabout(
                            canvas,
                            primaryColor,
                            secondaryColor,
                            size,
                            roundaboutAngle
                        )
                    })
                put(Pair(STEP_MANEUVER_TYPE_ROTARY, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawRoundabout(
                            canvas,
                            primaryColor,
                            secondaryColor,
                            size,
                            roundaboutAngle
                        )
                    })
                put(Pair(STEP_MANEUVER_TYPE_EXIT_ROTARY, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawRoundabout(
                            canvas,
                            primaryColor,
                            secondaryColor,
                            size,
                            roundaboutAngle
                        )
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrive(canvas, primaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_STRAIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrive(canvas, primaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_RIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size)
                    })
                put(Pair(STEP_MANEUVER_TYPE_ARRIVE, STEP_MANEUVER_MODIFIER_LEFT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_RIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SHARP_RIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SLIGHT_LEFT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_LEFT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_SHARP_LEFT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_UTURN),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrow180Right(canvas, primaryColor, size)
                    })
                put(Pair(null, STEP_MANEUVER_MODIFIER_STRAIGHT),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size)
                    })
                put(Pair(null, null),
                    ManeuverIconDrawer { canvas, primaryColor, secondaryColor, size, roundaboutAngle ->
                        ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size)
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
