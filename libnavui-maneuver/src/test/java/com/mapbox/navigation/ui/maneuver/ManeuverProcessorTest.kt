package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
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
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "LegStep should have valid banner instructions"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure("RouteLeg should have valid steps")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json")
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure("Route should have valid legs")

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route having empty banner instructions`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_empty_banner_instructions.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "Maneuver list not found corresponding to $routeLeg"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route and route leg passed is different`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = mockk<RouteLeg> {
            every { distance() } returns null
            every { duration() } returns null
            every { durationTypical() } returns null
            every { summary() } returns null
            every { admins() } returns null
            every { steps() } returns null
            every { incidents() } returns null
            every { annotation() } returns null
            every { closures() } returns null
        }
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverList.Failure(
            "$routeLeg passed is different"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with direction route and is valid`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(expected, actual.maneuvers.size)
    }

    @Test
    fun `when maneuver with direction route is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg = null
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLeg,
            maneuverState,
            distanceFormatter
        )

        ManeuverProcessor.process(maneuverAction)

        val route1 = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeLeg1 = null
        val maneuverAction1 = ManeuverAction.GetManeuverListWithRoute(
            route1,
            routeLeg1,
            maneuverState,
            distanceFormatter
        )

        val actual = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverList.Success

        assertEquals(21.2, actual.maneuvers[0].stepDistance.totalDistance, 0.0)
    }

    @Test
    fun `when maneuver with route progress having invalid banner instruction`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_banner_instruction.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = null,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "instructionIndex is null"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid steps`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_steps.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "RouteLeg should have valid steps"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having invalid legs`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route_invalid_legs.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "routeLeg is null"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different route leg`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 45f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val legProgress = routeProgress.currentLegProgress
        every { legProgress?.routeLeg } returns RouteLeg.builder().build()
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find the ${legProgress?.routeLeg}"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress having different step index`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 45f,
            _routeLegIndex = 0,
            _stepIndex = -1,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = ManeuverResult.GetManeuverListWithProgress.Failure(
            "Could not find the -1"
        )

        val actual = ManeuverProcessor.process(maneuverAction)

        assertEquals(expected, actual)
    }

    @Test
    fun `when maneuver with route progress valid`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        val expected = 4

        val actual = ManeuverProcessor.process(maneuverAction) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(expected, actual.maneuvers.size)
        assertEquals(15.0, actual.maneuvers[0].stepDistance.distanceRemaining)
    }

    @Test
    fun `when maneuver with route progress is fetched then call again`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("short_route.json")
        )
        val routeProgress = mockRouteProgress(
            _route = route,
            _distanceRemaining = 15f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverState = ManeuverState()
        val distanceFormatter = mockk<DistanceFormatter>()
        val maneuverAction = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )

        ManeuverProcessor.process(maneuverAction)

        val routeProgress1 = mockRouteProgress(
            _route = route,
            _distanceRemaining = 10f,
            _routeLegIndex = 0,
            _stepIndex = 0,
            _instructionIndex = 0,
        )
        val maneuverAction1 = ManeuverAction.GetManeuverList(
            routeProgress1,
            maneuverState,
            distanceFormatter
        )

        val actual1 = ManeuverProcessor.process(maneuverAction1) as
            ManeuverResult.GetManeuverListWithProgress.Success

        assertEquals(4, actual1.maneuvers.size)
        assertEquals(10.0, actual1.maneuvers[0].stepDistance.distanceRemaining)
    }

    private fun mockRouteProgress(
        _route: DirectionsRoute,
        _distanceRemaining: Float,
        _routeLegIndex: Int,
        _stepIndex: Int,
        _instructionIndex: Int?
    ): RouteProgress {
        return mockk {
            every { route } returns _route
            every { currentLegProgress } returns mockk {
                every { routeLeg } returns route.legs()?.getOrNull(_routeLegIndex)
                every { currentStepProgress } returns mockk {
                    every { step } returns
                        route.legs()?.getOrNull(_routeLegIndex)?.steps()?.getOrNull(_stepIndex)
                    every { stepIndex } returns _stepIndex
                    every { instructionIndex } returns _instructionIndex
                    every { distanceRemaining } returns _distanceRemaining
                }
            }
        }
    }
}
