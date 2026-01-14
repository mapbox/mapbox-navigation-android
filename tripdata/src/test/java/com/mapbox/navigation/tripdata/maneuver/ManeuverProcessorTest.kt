package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverOptions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `when maneuver with direction route having invalid banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json"),
        )
        val maneuverState = ManeuverState()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "LegStep should have valid banner instructions",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json"),
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverList.Failure("RouteLeg should have valid steps")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json"),
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverList.Failure("Route should have valid legs")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having empty banner instructions`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_empty_banner_instructions.json"),
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "no maneuvers available for the current route or its leg",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route and route leg passed is different`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeLegIndex = 2
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "provided leg for which maneuvers should be generated is not found in the route",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route is valid and filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeLegIndex = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 3

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `when maneuver with direction route and is valid and don't filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeLegIndex = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `when maneuver with direction route is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        ManeuverProcessor.process(maneuverAction)

        val route1 = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeLeg1 = null
        val maneuverAction1 = ManeuverAction.GetManeuverListWithRoute(
            route1,
            routeLeg1,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(21.2, actual.maneuvers[0].stepDistance.totalDistance, 0.0)
    }

    @Test
    fun `when maneuver with route progress having invalid banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = null,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "instructionIndex is null",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "RouteLeg should have valid steps",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Route should have valid legs",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different route leg index`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 45f,
            _routeLegIndex = 2,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find leg with index 2",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different step index`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 45f,
            _routeLegIndex = 0,
            _stepIndex = -1,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find step with index -1",
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress is valid and filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 3

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with significant route progress is valid and filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 1,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 2

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with route progress is valid and don't filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with significant route progress is valid and don't filter duplicate`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 1,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 2

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with cycling profile then active direction fallbacks to primary modifier`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_cycling.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success
        val activeDirection = actual.maneuvers[1].primary.modifier

        assertEquals("left", activeDirection)
    }

    @Test
    fun `when maneuver contains lane guidance information then expected lanes are added`() {
        val expectedLaneGuidanceValues = listOf("left", "straight", "straight", "right")
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("car_route_with_lane_guidance.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success
        val laneGuidanceValues = actual.maneuvers[2].laneGuidance!!.allLanes.flatMap {
            it.directions
        }

        assertEquals(expectedLaneGuidanceValues, laneGuidanceValues)
    }

    @Test
    fun `when maneuver with no banner driving side then fallback to step driving side`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success
        val primaryDrivingSide = actual.maneuvers[0].primary.drivingSide

        assertEquals("right", primaryDrivingSide)
    }

    @Test
    fun `when maneuver with route progress is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        ManeuverProcessor.process(maneuverAction)

        val routeProgress1 = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 10f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverAction1 = ManeuverAction.GetManeuverList(
            routeProgress1,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual1 = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(3, actual1.maneuvers.size)
        assertEquals(10.0, actual1.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with route progress and changed annotations is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        ManeuverProcessor.process(maneuverAction)

        val transformedRoute = route.toBuilder().legs(
            route.legs()?.map { leg ->
                leg.toBuilder().annotation(
                    leg.annotation()?.toBuilder()?.congestion(
                        leg.annotation()?.congestion()?.map {
                            "moderate"
                        },
                    )?.build(),
                ).build()
            },
        ).build()

        val routeProgress1 = mockRouteProgress(
            _route = transformedRoute,
            _distanceRemainingOnStep = 10f,
            _routeLegIndex = 0,
            _stepIndex = 2,
            _instructionIndex = 0,
        )
        val maneuverAction1 = ManeuverAction.GetManeuverList(
            routeProgress1,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual1 = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(1, actual1.maneuvers.size)
        assertEquals(10.0, actual1.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `multileg with direction route defaults to first leg without duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeLegIndex = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `multileg with direction route respects leg index without duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeLegIndex = 1
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 14

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `multileg with direction route defaults to first leg with duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeLegIndex = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 5

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `multileg with direction route respects leg index with duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeLegIndex = 1
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 16

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `multileg route with leg index with route progress without duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 1,
            _stepIndex = 1,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(true).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 13

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `multileg route with leg and instruction index with route progress with duplicates`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_multileg_route.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 1,
            _stepIndex = 13,
            _instructionIndex = 1,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        val expected = 1

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver contains lane access information for bus then expected information is extracted`() {
        val expectedAccessDesignated = listOf("bus")
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("car_route_with_lane_guidance_and_access_bus.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success
        val actualAccessDesignated = actual.maneuvers[0].laneGuidance!!.allLanes[1].accessDesignated

        assertEquals(expectedAccessDesignated, actualAccessDesignated)
    }

    @Test
    fun `when maneuver contains lane access information for hov then expected information is extracted`() {
        val expectedAccessDesignated = listOf("hov")
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("car_route_with_lane_guidance_and_access_hov.json"),
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemainingOnStep = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverOptions = ManeuverOptions.Builder().filterDuplicateManeuvers(false).build()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success
        val actualAccessDesignated = actual.maneuvers[1].laneGuidance!!.allLanes[5].accessDesignated

        assertEquals(expectedAccessDesignated, actualAccessDesignated)
    }

    private fun mockRouteProgress(
        _route: DirectionsRoute,
        _distanceRemainingOnStep: Float,
        _routeLegIndex: Int,
        _stepIndex: Int,
        _instructionIndex: Int?,
    ): RouteProgress {
        return mockk {
            every { route } returns _route
            every { currentLegProgress } returns mockk {
                every { routeLeg } returns route.legs()?.getOrNull(_routeLegIndex)
                every { legIndex } returns _routeLegIndex
                every { currentStepProgress } returns mockk {
                    every { step } returns
                        route.legs()?.getOrNull(_routeLegIndex)?.steps()?.getOrNull(_stepIndex)
                    every { stepIndex } returns _stepIndex
                    every { instructionIndex } returns _instructionIndex
                    every { distanceRemaining } returns _distanceRemainingOnStep
                }
            }
        }
    }
}
