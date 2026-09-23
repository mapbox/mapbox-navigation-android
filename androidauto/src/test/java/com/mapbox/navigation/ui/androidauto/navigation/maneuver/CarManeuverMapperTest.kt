package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
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
            every { drivingSide } returns null
            every { componentList } returns emptyList()
        }
    }

    private val oneHourMilliseconds: Double = 60000.0 * 60.0
    private val currentStepDistanceMeters = 250f
    private val currentStepDurationSeconds = 45.0
    private val mockCurrentStepProgress = mockk<RouteStepProgress> {
        every { distanceRemaining } returns currentStepDistanceMeters
        every { durationRemaining } returns currentStepDurationSeconds
    }
    private val mockCurrentLegProgress = mockk<RouteLegProgress> {
        every { currentStepProgress } returns mockCurrentStepProgress
    }

    private val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
        every { durationRemaining } returns oneHourMilliseconds
        every { distanceRemaining } returns 1609.34f
        every { currentLegProgress } returns mockCurrentLegProgress
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
    fun `generate Trip data from route progress uses current-step estimate for the step`() {
        val expectedEta = Calendar.getInstance().also {
            it.timeInMillis = it.timeInMillis + (currentStepDurationSeconds * 1000).toLong()
        }
        val stepDistance = mockk<Distance>()
        val destinationDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(currentStepDistanceMeters.toDouble())
        } returns stepDistance
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns destinationDistance

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        val timeDelta = expectedEta.timeInMillis - trip.stepTravelEstimates
            .first()
            .arrivalTimeAtDestination!!
            .timeSinceEpochMillis
        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, trip.steps.first().maneuver!!.type)
        assertEquals(stepDistance, trip.stepTravelEstimates.first().remainingDistance!!)
        assertEquals(
            currentStepDurationSeconds.toLong(),
            trip.stepTravelEstimates.first().remainingTimeSeconds,
        )
        assertTrue(timeDelta < 20)
    }

    @Test
    fun `generate Trip data from route progress uses whole-route estimate for the destination`() {
        val expectedEta = Calendar.getInstance().also {
            it.timeInMillis = it.timeInMillis + mockRouteProgress.durationRemaining.toLong()
        }
        val destinationDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(currentStepDistanceMeters.toDouble())
        } returns mockk()
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns destinationDistance

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        val timeDelta = expectedEta.timeInMillis - trip.destinationTravelEstimates
            .first()
            .arrivalTimeAtDestination!!
            .timeSinceEpochMillis
        assertEquals(
            destinationDistance,
            trip.destinationTravelEstimates.first().remainingDistance!!,
        )
        assertEquals(
            oneHourMilliseconds.toLong(),
            trip.destinationTravelEstimates.first().remainingTimeSeconds,
        )
        assertTrue(timeDelta < 20)
    }

    @Test
    fun `falls back to whole-route estimate when current-step progress is unavailable`() {
        every { mockCurrentLegProgress.currentStepProgress } returns null
        val destinationDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns destinationDistance

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        assertEquals(destinationDistance, trip.stepTravelEstimates.first().remainingDistance!!)
        assertEquals(
            oneHourMilliseconds.toLong(),
            trip.stepTravelEstimates.first().remainingTimeSeconds,
        )
    }

    @Test
    fun `falls back to whole-route estimate when current leg progress is unavailable`() {
        every { mockRouteProgress.currentLegProgress } returns null
        val destinationDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns destinationDistance

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        assertEquals(destinationDistance, trip.stepTravelEstimates.first().remainingDistance!!)
    }

    @Test
    fun `reports remaining time unknown when current-step duration is unavailable`() {
        every { mockCurrentStepProgress.durationRemaining } returns -1.0
        every {
            CarDistanceFormatter.carDistance(currentStepDistanceMeters.toDouble())
        } returns mockk()
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns mockk()

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        assertEquals(
            TravelEstimate.REMAINING_TIME_UNKNOWN,
            trip.stepTravelEstimates.first().remainingTimeSeconds,
        )
    }

    @Test
    fun `uses current-step estimate regardless of which leg is active on a multi-leg route`() {
        every { mockCurrentLegProgress.legIndex } returns 1
        val stepDistance = mockk<Distance>()
        every {
            CarDistanceFormatter.carDistance(currentStepDistanceMeters.toDouble())
        } returns stepDistance
        every {
            CarDistanceFormatter.carDistance(range(1609.34 - 0.1, 1609.34 + 0.1))
        } returns mockk()

        val trip = CarManeuverMapper.from(mockRouteProgress, mockManeuverApi)

        assertEquals(stepDistance, trip.stepTravelEstimates.first().remainingDistance!!)
    }

    @Test
    fun `generate Trip data from expected`() {
        val expected = ExpectedFactory.createValue<
            ManeuverError,
            List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>,>(listOf(mockManeuver))

        val maneuver = CarManeuverMapper.from(expected).build()

        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, maneuver.type)
    }

    @Test
    fun `generate roundabout maneuver for right driving side without angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.RIGHT,
            degrees = null,
            drivingSide = "right",
            roundaboutExitNumber = 2,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
        assertEquals(2, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver for left driving side without angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.LEFT,
            degrees = null,
            drivingSide = "left",
            roundaboutExitNumber = 3,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW, actual.type)
        assertEquals(3, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver for right driving side with angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT_TURN,
            ManeuverModifier.STRAIGHT,
            degrees = 180.0,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE, actual.type)
        assertEquals(180, actual.roundaboutExitAngle)
    }

    @Test
    fun `generate roundabout maneuver for left driving side with angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.EXIT_ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = 270.0,
            drivingSide = "left",
            roundaboutExitNumber = 4,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE, actual.type)
        assertEquals(270, actual.roundaboutExitAngle)
        assertEquals(4, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver falls back to counter-clockwise for missing driving side`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROTARY,
            ManeuverModifier.RIGHT,
            degrees = null,
            drivingSide = null,
            roundaboutExitNumber = null,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
        assertEquals(1, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver falls back to counter-clockwise for malformed driving side`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROTARY,
            ManeuverModifier.RIGHT,
            degrees = null,
            drivingSide = "sideways",
            roundaboutExitNumber = 2,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
    }

    @Test
    fun `generate roundabout maneuver falls back to default exit number when missing`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.EXIT_ROTARY,
            ManeuverModifier.LEFT,
            degrees = null,
            drivingSide = "left",
            roundaboutExitNumber = null,
        ).build()

        assertEquals(1, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver falls back to default exit number when malformed`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.LEFT,
            degrees = null,
            drivingSide = "left",
            roundaboutExitNumber = 0,
        ).build()

        assertEquals(1, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver ignores angle that is zero`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = 0.0,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
    }

    @Test
    fun `generate roundabout maneuver ignores angle greater than 360`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = 361.0,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
    }

    @Test
    fun `generate roundabout maneuver ignores negative angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = -45.0,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
    }

    @Test
    fun `generate roundabout maneuver accepts minimum valid angle of 1 degree`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = 1.0,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE, actual.type)
        assertEquals(1, actual.roundaboutExitAngle)
    }

    @Test
    fun `generate roundabout maneuver accepts maximum valid angle of 360 degrees`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = 360.0,
            drivingSide = "left",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE, actual.type)
        assertEquals(360, actual.roundaboutExitAngle)
    }

    @Test
    fun `generate roundabout maneuver does not crash and ignores NaN angle`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.STRAIGHT,
            degrees = Double.NaN,
            drivingSide = "right",
            roundaboutExitNumber = 1,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
    }

    @Test
    fun `three-argument overload is generated on the JVM for pre-existing Java callers`() {
        val method = CarManeuverMapper::class.java.getMethod(
            "from",
            String::class.java,
            String::class.java,
            java.lang.Double::class.java,
        )

        val actual = method.invoke(
            CarManeuverMapper,
            StepManeuver.DEPART,
            ManeuverModifier.LEFT,
            null,
        ) as Maneuver.Builder

        assertEquals(Maneuver.TYPE_DEPART, actual.build().type)
    }

    @Test
    fun `generate roundabout maneuver reads exit number and driving side from expected maneuver`() {
        val exitNumberNode = mockk<ExitNumberComponentNode> {
            every { text } returns "3"
        }
        val roundaboutManeuver = mockk<com.mapbox.navigation.tripdata.maneuver.model.Maneuver> {
            every { primary } returns mockk {
                every { type } returns StepManeuver.ROUNDABOUT
                every { modifier } returns ManeuverModifier.RIGHT
                every { degrees } returns null
                every { drivingSide } returns "left"
                every { componentList } returns listOf(
                    Component(BannerComponents.EXIT_NUMBER, exitNumberNode),
                )
            }
        }
        val expected = ExpectedFactory.createValue<
            ManeuverError,
            List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>,>(
            listOf(roundaboutManeuver),
        )

        val actual = CarManeuverMapper.from(expected).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW, actual.type)
        assertEquals(3, actual.roundaboutExitNumber)
    }

    @Test
    fun `generate roundabout maneuver falls back to default exit number when component is malformed`() {
        val exitNumberNode = mockk<ExitNumberComponentNode> {
            every { text } returns "not-a-number"
        }
        val roundaboutManeuver = mockk<com.mapbox.navigation.tripdata.maneuver.model.Maneuver> {
            every { primary } returns mockk {
                every { type } returns StepManeuver.ROUNDABOUT
                every { modifier } returns ManeuverModifier.RIGHT
                every { degrees } returns null
                every { drivingSide } returns "right"
                every { componentList } returns listOf(
                    Component(BannerComponents.EXIT_NUMBER, exitNumberNode),
                )
            }
        }
        val expected = ExpectedFactory.createValue<
            ManeuverError,
            List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>,>(
            listOf(roundaboutManeuver),
        )

        val actual = CarManeuverMapper.from(expected).build()

        assertEquals(1, actual.roundaboutExitNumber)
    }
}
