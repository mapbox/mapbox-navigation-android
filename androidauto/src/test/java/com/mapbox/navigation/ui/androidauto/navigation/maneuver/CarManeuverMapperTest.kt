package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.FileUtils
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
import org.junit.Assert.assertNull
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
        every { stepIndex } returns 0
        every { distanceRemaining } returns currentStepDistanceMeters
        every { durationRemaining } returns currentStepDurationSeconds
    }
    private val mockCurrentLegProgress = mockk<RouteLegProgress> {
        every { routeLeg } returns null
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

    @Test
    fun `generate turn icon with merge type and null modifier`() {
        val actual = CarManeuverMapper.from(StepManeuver.MERGE, null).build()

        assertEquals(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED, actual.type)
    }

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

    @Test
    fun `generate turn icon with end of road type and right modifier`() {
        val actual =
            CarManeuverMapper.from(StepManeuver.END_OF_ROAD, ManeuverModifier.RIGHT).build()

        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, actual.type)
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
            StepManeuver.ROUNDABOUT,
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
            StepManeuver.ROTARY,
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

    @Test
    fun `roundabout exit number comes from the step the banner announces`() {
        // The first step's banner announces the roundabout that starts the second step, which
        // takes the 3rd exit. The banner itself has no exit number component.
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("directions_route_with_roundabout_exits.json"),
        )
        val routeProgress = routeProgressOn(route.legs()!![0], stepIndex = 0)
        val banner = route.legs()!![0].steps()!![0].bannerInstructions()!!.first().primary()

        val exitNumber = CarManeuverMapper.roundaboutExitNumber(
            routeProgress,
            CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
            componentList = emptyList(),
        )
        val actual = CarManeuverMapper.from(
            banner.type(),
            banner.modifier(),
            banner.degrees(),
            banner.drivingSide(),
            exitNumber,
        ).build()

        assertEquals(3, exitNumber)
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE, actual.type)
        assertEquals(3, actual.roundaboutExitNumber)
        assertEquals(173, actual.roundaboutExitAngle)
    }

    @Test
    fun `trip uses the roundabout exit number of the announced step`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("directions_route_with_roundabout_exits.json"),
        )
        val routeProgress = routeProgressOn(route.legs()!![0], stepIndex = 0)
        val roundaboutManeuver = mockk<com.mapbox.navigation.tripdata.maneuver.model.Maneuver> {
            every { primary } returns mockk {
                every { type } returns StepManeuver.ROUNDABOUT
                every { modifier } returns ManeuverModifier.RIGHT
                every { degrees } returns null
                every { drivingSide } returns "right"
                every { componentList } returns emptyList()
            }
        }
        val maneuverApi = mockk<MapboxManeuverApi> {
            every { getManeuvers(routeProgress) } returns
                ExpectedFactory.createValue(listOf(roundaboutManeuver))
        }
        every { CarDistanceFormatter.carDistance(any()) } returns mockk()

        val trip = CarManeuverMapper.from(routeProgress, maneuverApi, "Destination")

        val maneuver = trip.steps.first().maneuver!!
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, maneuver.type)
        assertEquals(3, maneuver.roundaboutExitNumber)
    }

    @Test
    fun `roundabout exit number ignores a step that is not a roundabout`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("directions_route_with_roundabout_exits.json"),
        )
        // The step after the roundabout is the arrival, which has no exit to take.
        val routeProgress = routeProgressOn(route.legs()!![0], stepIndex = 1)

        val exitNumber = CarManeuverMapper.roundaboutExitNumber(
            routeProgress,
            CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
            componentList = listOf(exitNumberComponent("2")),
        )

        assertEquals(2, exitNumber)
    }

    @Test
    fun `sub-maneuver roundabout exit number comes from two steps ahead`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("directions_route_with_roundabout_exits.json"),
        )
        // Two steps before the roundabout: the sub-maneuver of the first one announces it.
        val leg = route.legs()!![0]
        val steps = leg.steps()!!
        val legWithExtraStep = leg.toBuilder()
            .steps(listOf(steps[0], steps[0], steps[1], steps[2]))
            .build()
        val routeProgress = routeProgressOn(legWithExtraStep, stepIndex = 0)

        val exitNumber = CarManeuverMapper.roundaboutExitNumber(
            routeProgress,
            CarManeuverMapper.SUB_MANEUVER_STEP_OFFSET,
            componentList = emptyList(),
        )

        assertEquals(3, exitNumber)
    }

    @Test
    fun `roundabout exit number falls back to the banner component without route progress`() {
        val exitNumber = CarManeuverMapper.roundaboutExitNumber(
            routeProgress = null,
            CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
            componentList = listOf(exitNumberComponent("4")),
        )

        assertEquals(4, exitNumber)
    }

    @Test
    fun `exit roundabout maps to a counter-clockwise roundabout exit in right-hand traffic`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.EXIT_ROUNDABOUT,
            ManeuverModifier.SLIGHT_RIGHT,
            degrees = 90.0,
            drivingSide = "right",
            roundaboutExitNumber = 2,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW, actual.type)
    }

    @Test
    fun `exit rotary maps to a clockwise roundabout exit in left-hand traffic`() {
        val actual = CarManeuverMapper.from(
            StepManeuver.EXIT_ROTARY,
            ManeuverModifier.SLIGHT_LEFT,
            drivingSide = "left",
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CW, actual.type)
    }

    @Test
    fun `destination name prefers the requested name of the final waypoint`() {
        val routeProgress = routeProgressTo(
            waypointNames = listOf("Home", "Coffee shop", "Office"),
            waypoints = listOf(waypoint("Origin Road"), waypoint("Final Avenue")),
        )

        assertEquals("Office", CarManeuverMapper.destinationName(routeProgress))
    }

    @Test
    fun `destination name falls back to the snapped waypoint name`() {
        val withoutRequestedNames = routeProgressTo(
            waypointNames = null,
            waypoints = listOf(waypoint("Origin Road"), waypoint("Final Avenue")),
        )
        // A list that leaves the destination unnamed ends with an empty name.
        val withUnnamedDestination = routeProgressTo(
            waypointNames = listOf("Home", ""),
            waypoints = listOf(waypoint("Origin Road"), waypoint("Final Avenue")),
        )

        assertEquals("Final Avenue", CarManeuverMapper.destinationName(withoutRequestedNames))
        assertEquals("Final Avenue", CarManeuverMapper.destinationName(withUnnamedDestination))
    }

    @Test
    fun `destination name is null without a named final waypoint`() {
        val withoutWaypoints = routeProgressTo(waypointNames = null, waypoints = null)
        val withBlankName = routeProgressTo(waypointNames = null, waypoints = listOf(waypoint("")))

        assertNull(CarManeuverMapper.destinationName(withoutWaypoints))
        assertNull(CarManeuverMapper.destinationName(withBlankName))
    }

    @Test
    fun `roundabout banner announcing a roundabout exit maps to a roundabout exit`() {
        // The second leg's first banner says "roundabout", but the step it announces only
        // leaves the roundabout, so there is no exit number to take.
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("directions_route_with_roundabout_exits.json"),
        )
        val routeProgress = routeProgressOn(route.legs()!![1], stepIndex = 0)
        val banner = route.legs()!![1].steps()!![0].bannerInstructions()!!.first().primary()

        val actual = CarManeuverMapper.fromAnnouncedStep(
            banner.type(),
            banner.modifier(),
            banner.degrees(),
            banner.drivingSide(),
            componentList = emptyList(),
            routeProgress,
            CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
        ).build()

        assertEquals(StepManeuver.ROUNDABOUT, banner.type())
        assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW, actual.type)
    }

    @Test
    fun `roundabout banner keeps its type without route progress`() {
        val actual = CarManeuverMapper.fromAnnouncedStep(
            StepManeuver.ROUNDABOUT,
            ManeuverModifier.RIGHT,
            degrees = null,
            drivingSide = "right",
            componentList = listOf(exitNumberComponent("3")),
            routeProgress = null,
            CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
        ).build()

        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, actual.type)
        assertEquals(3, actual.roundaboutExitNumber)
    }

    private fun routeProgressTo(
        waypointNames: List<String>?,
        waypoints: List<DirectionsWaypoint>?,
    ): RouteProgress = mockk {
        every { navigationRoute } returns mockk {
            every { directionsRoute.routeOptions() } returns mockk {
                every { waypointNamesList() } returns waypointNames
            }
            every { this@mockk.waypoints } returns waypoints
        }
    }

    private fun routeProgressOn(leg: RouteLeg, stepIndex: Int): RouteProgress {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { this@mockk.stepIndex } returns stepIndex
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { routeLeg } returns leg
            every { currentStepProgress } returns stepProgress
        }
        return mockk(relaxed = true) {
            every { currentLegProgress } returns legProgress
        }
    }

    private fun exitNumberComponent(text: String) = Component(
        BannerComponents.EXIT_NUMBER,
        ExitNumberComponentNode.Builder().text(text).build(),
    )

    private fun waypoint(name: String) = DirectionsWaypoint.builder()
        .name(name)
        .rawLocation(doubleArrayOf(0.0, 0.0))
        .build()
}
