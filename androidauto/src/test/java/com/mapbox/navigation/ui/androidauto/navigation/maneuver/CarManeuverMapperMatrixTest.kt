package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.navigation.model.Maneuver
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Pins the host maneuver type for every [StepManeuver] type, [ManeuverModifier] and driving
 * side. The host draws the cluster and trip maneuvers from this type alone.
 */
@RunWith(Parameterized::class)
class CarManeuverMapperMatrixTest(
    private val maneuverType: String?,
    private val modifier: String?,
    private val drivingSide: String?,
    private val expectedType: Int,
) {

    @Test
    fun `maps to the expected host maneuver type`() {
        val actual = CarManeuverMapper.from(
            maneuverType,
            modifier,
            degrees = null,
            drivingSide = drivingSide,
            roundaboutExitNumber = ROUNDABOUT_EXIT_NUMBER,
        ).build()

        assertEquals(expectedType, actual.type)
    }

    companion object {
        private const val ROUNDABOUT_EXIT_NUMBER = 2
        private const val UNKNOWN_TYPE = "unknown type"

        private val MODIFIERS = listOf(
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            null,
        )

        /** Driving sides, where null and unknown values mean right-hand traffic. */
        private val DRIVING_SIDES = listOf("right", "left", null, "unknown side")

        private val TURNS = mapOf(
            ManeuverModifier.STRAIGHT to Maneuver.TYPE_STRAIGHT,
            ManeuverModifier.RIGHT to Maneuver.TYPE_TURN_NORMAL_RIGHT,
            ManeuverModifier.SLIGHT_RIGHT to Maneuver.TYPE_TURN_SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT to Maneuver.TYPE_TURN_SHARP_RIGHT,
            ManeuverModifier.LEFT to Maneuver.TYPE_TURN_NORMAL_LEFT,
            ManeuverModifier.SLIGHT_LEFT to Maneuver.TYPE_TURN_SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT to Maneuver.TYPE_TURN_SHARP_LEFT,
        )

        private fun isLeftHandTraffic(drivingSide: String?) = drivingSide == "left"

        private fun uTurn(drivingSide: String?) = if (isLeftHandTraffic(drivingSide)) {
            Maneuver.TYPE_U_TURN_RIGHT
        } else {
            Maneuver.TYPE_U_TURN_LEFT
        }

        private fun expected(type: String?, modifier: String?, drivingSide: String?): Int {
            val lht = isLeftHandTraffic(drivingSide)
            return when (type) {
                StepManeuver.TURN,
                StepManeuver.NEW_NAME,
                StepManeuver.NOTIFICATION,
                null,
                UNKNOWN_TYPE,
                -> when (modifier) {
                    ManeuverModifier.UTURN -> uTurn(drivingSide)
                    else -> TURNS[modifier] ?: Maneuver.TYPE_STRAIGHT
                }

                StepManeuver.END_OF_ROAD -> when (modifier) {
                    ManeuverModifier.UTURN -> uTurn(drivingSide)
                    else -> TURNS[modifier] ?: Maneuver.TYPE_UNKNOWN
                }

                StepManeuver.DEPART -> Maneuver.TYPE_DEPART
                StepManeuver.CONTINUE -> Maneuver.TYPE_STRAIGHT

                StepManeuver.ARRIVE -> when (modifier) {
                    ManeuverModifier.STRAIGHT -> Maneuver.TYPE_DESTINATION_STRAIGHT
                    ManeuverModifier.RIGHT,
                    ManeuverModifier.SLIGHT_RIGHT,
                    ManeuverModifier.SHARP_RIGHT,
                    -> Maneuver.TYPE_DESTINATION_RIGHT
                    ManeuverModifier.LEFT,
                    ManeuverModifier.SLIGHT_LEFT,
                    ManeuverModifier.SHARP_LEFT,
                    -> Maneuver.TYPE_DESTINATION_LEFT
                    else -> Maneuver.TYPE_DESTINATION
                }

                StepManeuver.MERGE -> when (modifier) {
                    ManeuverModifier.RIGHT,
                    ManeuverModifier.SLIGHT_RIGHT,
                    ManeuverModifier.SHARP_RIGHT,
                    -> Maneuver.TYPE_MERGE_RIGHT
                    ManeuverModifier.LEFT,
                    ManeuverModifier.SLIGHT_LEFT,
                    ManeuverModifier.SHARP_LEFT,
                    -> Maneuver.TYPE_MERGE_LEFT
                    else -> Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED
                }

                StepManeuver.ON_RAMP -> when (modifier) {
                    ManeuverModifier.UTURN,
                    ManeuverModifier.STRAIGHT,
                    -> Maneuver.TYPE_STRAIGHT
                    ManeuverModifier.RIGHT -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
                    ManeuverModifier.SLIGHT_RIGHT -> Maneuver.TYPE_ON_RAMP_SLIGHT_RIGHT
                    ManeuverModifier.SHARP_RIGHT -> Maneuver.TYPE_ON_RAMP_SHARP_RIGHT
                    ManeuverModifier.LEFT -> Maneuver.TYPE_ON_RAMP_NORMAL_LEFT
                    ManeuverModifier.SLIGHT_LEFT -> Maneuver.TYPE_ON_RAMP_SLIGHT_LEFT
                    ManeuverModifier.SHARP_LEFT -> Maneuver.TYPE_ON_RAMP_SHARP_LEFT
                    else -> Maneuver.TYPE_UNKNOWN
                }

                StepManeuver.OFF_RAMP -> when (modifier) {
                    ManeuverModifier.UTURN,
                    ManeuverModifier.STRAIGHT,
                    -> if (lht) {
                        Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
                    } else {
                        Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
                    }
                    ManeuverModifier.RIGHT,
                    ManeuverModifier.SHARP_RIGHT,
                    -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
                    ManeuverModifier.SLIGHT_RIGHT -> Maneuver.TYPE_OFF_RAMP_SLIGHT_RIGHT
                    ManeuverModifier.LEFT,
                    ManeuverModifier.SHARP_LEFT,
                    -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
                    ManeuverModifier.SLIGHT_LEFT -> Maneuver.TYPE_OFF_RAMP_SLIGHT_LEFT
                    else -> Maneuver.TYPE_UNKNOWN
                }

                StepManeuver.FORK -> when (modifier) {
                    ManeuverModifier.UTURN,
                    ManeuverModifier.STRAIGHT,
                    -> if (lht) Maneuver.TYPE_FORK_LEFT else Maneuver.TYPE_FORK_RIGHT
                    ManeuverModifier.RIGHT,
                    ManeuverModifier.SLIGHT_RIGHT,
                    ManeuverModifier.SHARP_RIGHT,
                    -> Maneuver.TYPE_FORK_RIGHT
                    ManeuverModifier.LEFT,
                    ManeuverModifier.SLIGHT_LEFT,
                    ManeuverModifier.SHARP_LEFT,
                    -> Maneuver.TYPE_FORK_LEFT
                    else -> Maneuver.TYPE_UNKNOWN
                }

                StepManeuver.ROUNDABOUT,
                StepManeuver.ROUNDABOUT_TURN,
                StepManeuver.ROTARY,
                -> when {
                    modifier == null -> Maneuver.TYPE_UNKNOWN
                    lht -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW
                    else -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
                }

                StepManeuver.EXIT_ROUNDABOUT,
                StepManeuver.EXIT_ROTARY,
                -> if (lht) Maneuver.TYPE_ROUNDABOUT_EXIT_CW else Maneuver.TYPE_ROUNDABOUT_EXIT_CCW

                else -> error("No expectation for maneuver type $type")
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0} / {1} / driving side {2}")
        fun data(): List<Array<Any?>> {
            val types = listOf(
                StepManeuver.TURN,
                StepManeuver.NEW_NAME,
                StepManeuver.DEPART,
                StepManeuver.ARRIVE,
                StepManeuver.MERGE,
                StepManeuver.ON_RAMP,
                StepManeuver.OFF_RAMP,
                StepManeuver.FORK,
                StepManeuver.END_OF_ROAD,
                StepManeuver.CONTINUE,
                StepManeuver.ROUNDABOUT,
                StepManeuver.ROUNDABOUT_TURN,
                StepManeuver.ROTARY,
                StepManeuver.EXIT_ROUNDABOUT,
                StepManeuver.EXIT_ROTARY,
                StepManeuver.NOTIFICATION,
                null,
                UNKNOWN_TYPE,
            )
            return types.flatMap { type ->
                MODIFIERS.flatMap { modifier ->
                    DRIVING_SIDES.map { drivingSide ->
                        arrayOf(type, modifier, drivingSide, expected(type, modifier, drivingSide))
                    }
                }
            }
        }
    }
}
