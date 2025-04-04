package com.mapbox.navigation.tripdata.maneuver

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
import com.mapbox.api.directions.v5.models.StepManeuver.FORK
import com.mapbox.api.directions.v5.models.StepManeuver.MERGE
import com.mapbox.api.directions.v5.models.StepManeuver.OFF_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ON_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT_TURN
import com.mapbox.api.directions.v5.models.StepManeuver.TURN
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import com.mapbox.navigation.tripdata.R
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import org.junit.Assert.assertEquals
import org.junit.Test

class TurnIconHelperTest {

    private val turnIconHelper = TurnIconHelper(TurnIconResources.defaultIconSet())

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
            R.drawable.mapbox_ic_turn_straight,
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
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_left,
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
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_right,
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
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockModifier: String = UTURN
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_uturn,
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
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_right,
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
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_left,
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
        val mockModifier: String = SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_right,
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
        val mockModifier: String = SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_left,
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
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_arrive,
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
        val mockType: String = DEPART
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_depart,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_on_ramp,
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
        val mockType: String = OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_off_ramp,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String? = null
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_end_of_road_left,
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
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_arrive_left,
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
        val mockType: String = ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_arrive_right,
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
        val mockType: String = ARRIVE
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_arrive_straight,
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
        val mockType: String = DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_depart_left,
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
        val mockType: String = DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_depart_right,
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
        val mockType: String = DEPART
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_depart_straight,
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
        val mockType: String = END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_end_of_road_left,
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
        val mockType: String = END_OF_ROAD
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_end_of_road_right,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork_right,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork_left,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork_straight,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork_slight_left,
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
        val mockType: String = FORK
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_fork_slight_right,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_merge_right,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_merge_left,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_merge_slight_left,
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
        val mockType: String = MERGE
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_merge_slight_right,
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
        val mockType: String = OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_off_ramp_left,
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
        val mockType: String = OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_off_ramp_right,
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
        val mockType: String = OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_off_ramp_slight_left,
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
        val mockType: String = OFF_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_off_ramp_slight_right,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_right,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_left,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_left,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_right,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_left,
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
        val mockType: String = ON_RAMP
        val mockDegrees: Float? = null
        val mockModifier: String = SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_right,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_right,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_left,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = STRAIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_straight,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = UTURN
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_uturn,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_left,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = SLIGHT_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_slight_right,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = SHARP_LEFT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_left,
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
        val mockType: String = TURN
        val mockDegrees: Float? = null
        val mockModifier: String = SHARP_RIGHT
        val mockDrivingSide: String? = null
        val expected = ManeuverTurnIcon(
            mockDegrees,
            mockDrivingSide,
            false,
            R.drawable.mapbox_ic_turn_sharp_right,
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
            R.drawable.mapbox_ic_turn_straight,
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
    fun `generate roundabout icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(ROUNDABOUT, 210f, SLIGHT_RIGHT, "right")

        val expected =
            ManeuverTurnIcon(210f, "right", false, R.drawable.mapbox_ic_roundabout_slight_left)
        assertEquals(expected, actual)
    }

    @Test
    fun `DRIVING SIDE LEFT - should flip UTURN icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(TURN, null, UTURN, "left")

        val expected = ManeuverTurnIcon(null, "left", true, R.drawable.mapbox_ic_uturn)
        assertEquals(expected, actual)
    }

    @Test
    fun `DRIVING SIDE LEFT - should flip FORK icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(FORK, null, null, "left")

        val expected = ManeuverTurnIcon(null, "left", true, R.drawable.mapbox_ic_fork)
        assertEquals(expected, actual)
    }

    @Test
    fun `DRIVING SIDE LEFT - should flip FORK STRAIGHT icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(FORK, null, STRAIGHT, "left")

        val expected = ManeuverTurnIcon(null, "left", true, R.drawable.mapbox_ic_fork_straight)
        assertEquals(expected, actual)
    }

    @Test
    fun `DRIVING SIDE LEFT - should flip OFF_RAMP icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(OFF_RAMP, null, null, "left")

        val expected = ManeuverTurnIcon(null, "left", true, R.drawable.mapbox_ic_off_ramp)
        assertEquals(expected, actual)
    }

    @Test
    fun `DRIVING SIDE LEFT - should flip roundabout icon`() {
        val actual = turnIconHelper.retrieveTurnIcon(ROUNDABOUT_TURN, 34f, SHARP_RIGHT, "left")

        val expected =
            ManeuverTurnIcon(34f, "left", true, R.drawable.mapbox_ic_roundabout_sharp_right)
        assertEquals(expected, actual)
    }
}
