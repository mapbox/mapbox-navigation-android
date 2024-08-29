package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class CarManeuverMapperTest {

    private val mockManeuver = mockk<com.mapbox.navigation.tripdata.maneuver.model.Maneuver> {
        every { primary } returns mockk {
            every { type } returns StepManeuver.TURN
            every { modifier } returns "right"
            every { degrees } returns null
        }
    }

    private val oneHourMilliseconds: Double = 60000.0 * 60.0
    private val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
        every { durationRemaining } returns oneHourMilliseconds
        every { distanceRemaining } returns 1609.34f
    }

    private val mockManeuverApi = mockk<MapboxManeuverApi>(relaxed = true) {
        every {
            getManeuvers(mockRouteProgress)
        } returns ExpectedFactory.createValue(listOf(mockManeuver))
    }

    @Before
    fun setup() {
        mockkStatic(CarDistanceFormatter::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `generate turn icon when type and modifier is null`() {
        val actual = CarManeuverMapper.from(null, null).build()

        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and left modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_TURN_NORMAL_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and right modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and straight modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.STRAIGHT).build()

        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
    }

    // TODO identify left and right
//    @Test
//    fun `generate turn icon with null type and uturn modifier`() {
//        val actual = maneuverMapper.from(null, ManeuverModifier.UTURN)
//
//        assertEquals(Maneuver.TYPE_U_TURN_LEFT, actual.type)
//        assertEquals(Maneuver.TYPE_U_TURN_RIGHT, actual.type)
//    }F

    @Test
    fun `generate turn icon with null type and sight right modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.SLIGHT_RIGHT).build()

        assertEquals(Maneuver.TYPE_TURN_SLIGHT_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and sight left modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.SLIGHT_LEFT).build()

        assertEquals(Maneuver.TYPE_TURN_SLIGHT_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and sharp right modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.SHARP_RIGHT).build()

        assertEquals(Maneuver.TYPE_TURN_SHARP_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and sharp left modifier`() {
        val actual = CarManeuverMapper.from(null, ManeuverModifier.SHARP_LEFT).build()

        assertEquals(Maneuver.TYPE_TURN_SHARP_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with null type and invalid modifier`() {
        val actual = CarManeuverMapper.from(null, " ").build()

        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with arrive type and null modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.ARRIVE, " ")
//
//        assertEquals(Maneuver.TYPE_DESTINATION, actual.type)
//    }

    // TODO idenfity left and right
//    @Test
//    fun `generate turn icon with on ramp type and null modifier`() {
//        val actual = maneuverMapper.from(null, StepManeuver.ON_RAMP)
//
//        assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, actual.type)
//    }

    // TODO idenfity left and right
//    @Test
//    fun `generate turn icon with off ramp type and null modifier`() {
//        val actual = maneuverMapper.from(null, StepManeuver.OFF_RAMP)
//
//        assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, actual.type)
//    }

    // TODO idenfity left and right
//    @Test
//    fun `generate turn icon with fork type and null modifier`() {
//        val actual = maneuverMapper.from(null, StepManeuver.FORK)
//
//        assertEquals(Maneuver.TYPE_FORK_LEFT, actual.type)
//    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with turn type and null modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.TURN, null)
//
//        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
//    }

    // TODO idenfity left and right
    @Test
    fun `generate turn icon with merge type and null modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.MERGE, null).build()

        assertEquals(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED, actual.type)
    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with end road type and null modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.END_OF_ROAD, null)
//
//        assertEquals(Maneuver.TYPE_DESTINATION, actual.type)
//    }

    @Test
    fun `generate turn icon with invalid type and null modifier`() {
        val actual = CarManeuverMapper.from(" ", null).build()

        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with arrive type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ARRIVE, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_DESTINATION_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with arrive type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ARRIVE, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_DESTINATION_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with arrive type and straight modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ARRIVE, ManeuverModifier.STRAIGHT).build()

        assertEquals(Maneuver.TYPE_DESTINATION_STRAIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with depart type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.DEPART, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_DEPART, actual.type)
    }

    @Test
    fun `generate turn icon with depart type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.DEPART, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_DEPART, actual.type)
    }

    @Test
    fun `generate turn icon with depart type and straight modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.DEPART, ManeuverModifier.STRAIGHT).build()

        assertEquals(Maneuver.TYPE_DEPART, actual.type)
    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with end of road type and left modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.END_OF_ROAD, ManeuverModifier.LEFT)
//
//        assertEquals(Maneuver.TYPE_DESTINATION_LEFT, actual.type)
//    }

    // TODO needs verification
    @Test
    fun `generate turn icon with end of road type and right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.END_OF_ROAD, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_DESTINATION_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with fork type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.FORK, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_FORK_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with fork type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.FORK, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_FORK_LEFT, actual.type)
    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with fork type and straight modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.FORK, ManeuverModifier.STRAIGHT)
//
//        assertEquals(Maneuver.TYPE_FORK_LEFT, actual.type)
//    }

    @Test
    fun `generate turn icon with fork type and slight left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.FORK, ManeuverModifier.SLIGHT_LEFT).build()

        assertEquals(Maneuver.TYPE_FORK_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with fork type and slight right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.FORK, ManeuverModifier.SLIGHT_RIGHT).build()

        assertEquals(Maneuver.TYPE_FORK_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with merge type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.MERGE, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_MERGE_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with merge type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.MERGE, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_MERGE_LEFT, actual.type)
    }

    // TODO needs verification
//    @Test
//    fun `generate turn icon with merge type and straight modifier`() {
//        val actual = maneuverMapper.from(StepManeuver.MERGE, ManeuverModifier.STRAIGHT)
//
//        assertEquals(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED, actual.type)
//    }

    @Test
    fun `generate turn icon with merge type and slight left modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.MERGE, ManeuverModifier.SLIGHT_LEFT).build()

        assertEquals(Maneuver.TYPE_MERGE_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with merge type and slight right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.MERGE, ManeuverModifier.SLIGHT_RIGHT).build()

        assertEquals(Maneuver.TYPE_MERGE_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with off ramp type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.OFF_RAMP, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with off ramp type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.OFF_RAMP, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with off ramp type and slight left modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.OFF_RAMP, ManeuverModifier.SLIGHT_LEFT).build()

        assertEquals(Maneuver.TYPE_OFF_RAMP_SLIGHT_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with off ramp type and slight right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.OFF_RAMP, ManeuverModifier.SLIGHT_RIGHT).build()

        assertEquals(Maneuver.TYPE_OFF_RAMP_SLIGHT_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and straight modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.STRAIGHT).build()

        assertEquals(Maneuver.TYPE_STRAIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and slight left modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.SLIGHT_LEFT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_SLIGHT_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and slight right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.SLIGHT_RIGHT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_SLIGHT_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and sharp left modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.SHARP_LEFT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_SHARP_LEFT, actual.type)
    }

    @Test
    fun `generate turn icon with on ramp type and sharp right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.SHARP_RIGHT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_SHARP_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with turn type and right modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, actual.type)
    }

    @Test
    fun `generate turn icon with turn type and left modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.ON_RAMP, ManeuverModifier.LEFT).build()

        assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT, actual.type)
    }

    @Test
    fun `generate Trip data from route progress`() {
        val expectedEta = Calendar.getInstance().also {
            it.timeInMillis = it.timeInMillis + mockRouteProgress.durationRemaining.toLong()
        }
        val remainingDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns remainingDistance
        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        val timeDelta = expectedEta.timeInMillis - trip.stepTravelEstimates
            .first()
            .arrivalTimeAtDestination!!
            .timeSinceEpochMillis
        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, trip.steps.first().maneuver!!.type)
        assertEquals(remainingDistance, trip.stepTravelEstimates.first().remainingDistance!!)
        assertTrue(timeDelta < 20)
    }

    @Test
    fun `generate Trip data from expected`() {
        val expected = ExpectedFactory.createValue<
            ManeuverError,
            List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>,>(listOf(mockManeuver))

        val maneuver = CarManeuverMapper.from(expected).build()

        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, maneuver.type)
    }
}
