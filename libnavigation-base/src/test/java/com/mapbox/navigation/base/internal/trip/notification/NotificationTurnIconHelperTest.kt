package com.mapbox.navigation.base.internal.trip.notification

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.R
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTurnIconHelperTest {

    private val turnIconHelper = TurnIconHelper(NotificationTurnIconResources.defaultIconSet())

    @Test
    fun `generate turn icon when type and modifier is null`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and left modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and right modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and straight modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and uturn modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.UTURN
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_uturn,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and sight right modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and sight left modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and sharp right modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and sharp left modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with null type and invalid modifier`() {
        val mockType: String? = null
        val mockDegrees: Float? = null
        val mockModifier = " "
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with arrive type and null modifier`() {
        val mockType: String = StepManeuver.ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_arrive,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with depart type and null modifier`() {
        val mockType: String = StepManeuver.DEPART
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_depart,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and null modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_on_ramp,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with off ramp type and null modifier`() {
        val mockType: String = StepManeuver.OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_off_ramp,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and null modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and null modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and null modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with end road type and null modifier`() {
        val mockType: String = StepManeuver.END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_end_of_road_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with invalid type and null modifier`() {
        val mockType = " "
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with arrive type and left modifier`() {
        val mockType: String = StepManeuver.ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_arrive_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with arrive type and right modifier`() {
        val mockType: String = StepManeuver.ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_arrive_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with arrive type and straight modifier`() {
        val mockType: String = StepManeuver.ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_arrive_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with depart type and left modifier`() {
        val mockType: String = StepManeuver.DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_depart_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with depart type and right modifier`() {
        val mockType: String = StepManeuver.DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_depart_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with depart type and straight modifier`() {
        val mockType: String = StepManeuver.DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_depart_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with end of road type and left modifier`() {
        val mockType: String = StepManeuver.END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_end_of_road_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with end of road type and right modifier`() {
        val mockType: String = StepManeuver.END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_end_of_road_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and right modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and left modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and straight modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and slight left modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with fork type and slight right modifier`() {
        val mockType: String = StepManeuver.FORK
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_fork_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and right modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_merge_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and left modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_merge_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and straight modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and slight left modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_merge_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with merge type and slight right modifier`() {
        val mockType: String = StepManeuver.MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_merge_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with off ramp type and left modifier`() {
        val mockType: String = StepManeuver.OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_off_ramp_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with off ramp type and right modifier`() {
        val mockType: String = StepManeuver.OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_off_ramp_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with off ramp type and slight left modifier`() {
        val mockType: String = StepManeuver.OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_off_ramp_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with off ramp type and slight right modifier`() {
        val mockType: String = StepManeuver.OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_off_ramp_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and right modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and left modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and straight modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and slight left modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and slight right modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and sharp left modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with on ramp type and sharp right modifier`() {
        val mockType: String = StepManeuver.ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and right modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and left modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and straight modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and uturn modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.UTURN
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_uturn,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and slight left modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and slight right modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_slight_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and sharp left modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_left,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with turn type and sharp right modifier`() {
        val mockType: String = StepManeuver.TURN
        val mockDegrees: Float? = null
        val mockModifier: String = ManeuverModifier.SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_sharp_right,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `generate turn icon with invalid type and invalid modifier`() {
        val mockType: String = " "
        val mockDegrees: Float? = null
        val mockModifier: String = " "
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_notification_turn_straight,
        )

        val actual = turnIconHelper.retrieveTurnIcon(
            mockType,
            mockDegrees,
            mockModifier,
            mockDrivingSide,
        )

        assertEquals(expected, actual)
    }
}
