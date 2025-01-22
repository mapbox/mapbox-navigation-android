package com.mapbox.navigation.core.utils.search

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteStep
import com.mapbox.navigation.testing.factories.createStepIntersection
import com.mapbox.navigation.utils.internal.takeEvenly
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class SearchAlongRouteUtilsTest {

    @Test
    fun `selects all intersection points where no restrictions applied`() {
        assertEquals(
            collectPoints(*ALL_STEPS.toTypedArray()),
            SearchAlongRouteUtils.selectPoints(ROUTE_FULL),
        )
    }

    @Test
    fun `returns empty list for route with null legs`() {
        assertEquals(
            emptyList<Point>(),
            SearchAlongRouteUtils.selectPoints(createDirectionsRoute(legs = null)),
        )
    }

    @Test
    fun `filters out legs with null steps`() {
        val route = createDirectionsRoute(legs = listOf(LEG_0, createRouteLeg(steps = null)))
        assertEquals(
            collectPoints(STEP_0_0, STEP_0_1, STEP_0_2),
            SearchAlongRouteUtils.selectPoints(route),
        )
    }

    @Test
    fun `filters out steps with null intersections`() {
        val route = createDirectionsRoute(
            legs = listOf(
                LEG_0,
                createRouteLeg(steps = listOf(STEP_1_0, createRouteStep(intersections = null))),
            ),
        )
        assertEquals(
            collectPoints(STEP_0_0, STEP_0_1, STEP_0_2, STEP_1_0),
            SearchAlongRouteUtils.selectPoints(route),
        )
    }

    @Test
    fun `filters out intersections with ferry and tunnel classes`() {
        val intersection1 = Point.fromLngLat(15.0, 16.0)
        val intersection2 = Point.fromLngLat(16.0, 17.0)
        val intersection3 = Point.fromLngLat(17.0, 18.0)

        val intersections = listOf(
            createStepIntersection(
                location = intersection1,
                classes = listOf("toll", "restricted", "motorway"),
            ),
            createStepIntersection(location = intersection2, classes = listOf("ferry")),
            createStepIntersection(location = intersection3, classes = listOf("tunnel")),
        )

        val leg = createRouteLeg(
            steps = listOf(
                STEP_1_0,
                createRouteStep(intersections = intersections),
            ),
        )

        val route = createDirectionsRoute(legs = listOf(LEG_0, leg))
        assertEquals(
            collectPoints(STEP_0_0, STEP_0_1, STEP_0_2, STEP_1_0) + intersection1,
            SearchAlongRouteUtils.selectPoints(route),
        )
    }

    @Test
    fun `filters out intersections that are out of specified indices`() {
        val intersection1 = Point.fromLngLat(15.0, 16.0)
        val intersection2 = Point.fromLngLat(16.0, 17.0)
        val intersection3 = Point.fromLngLat(17.0, 18.0)

        val intersections = listOf(
            createStepIntersection(location = intersection1),
            createStepIntersection(location = intersection2),
            createStepIntersection(location = intersection3),
        )

        val leg1 = createRouteLeg(
            steps = listOf(
                STEP_1_0,
                createRouteStep(intersections = intersections),
            ),
        )

        val route = createDirectionsRoute(legs = listOf(LEG_0, leg1, LEG_2))

        assertEquals(
            listOf(intersection2, intersection3) + collectPoints(STEP_2_0),
            SearchAlongRouteUtils.selectPoints(route, 1, 1, 1),
        )
    }

    @Test
    fun `returns empty list for filtering indices out of ranges`() {
        assertEquals(
            emptyList<Point>(),
            SearchAlongRouteUtils.selectPoints(ROUTE_FULL, 10, 20, 30),
        )
    }

    @Test
    fun `evenly limits points`() {
        val limit = 5

        assertEquals(
            collectPoints(*ALL_STEPS.toTypedArray()).takeEvenly(limit),
            SearchAlongRouteUtils.selectPoints(ROUTE_FULL, limit = limit),
        )
    }

    @Test
    fun `returns correct points for RouteProgress`() {
        val intersection1 = Point.fromLngLat(15.0, 16.0)
        val intersection2 = Point.fromLngLat(16.0, 17.0)
        val intersection3 = Point.fromLngLat(17.0, 18.0)

        val intersections = listOf(
            createStepIntersection(location = intersection1),
            createStepIntersection(location = intersection2),
            createStepIntersection(location = intersection3),
        )

        val leg1 = createRouteLeg(
            steps = listOf(
                STEP_1_0,
                createRouteStep(intersections = intersections),
            ),
        )

        val route = createDirectionsRoute(legs = listOf(LEG_0, leg1, LEG_2))

        assertEquals(
            listOf(intersection2, intersection3) + collectPoints(STEP_2_0),
            SearchAlongRouteUtils.selectPoints(mockkRouteProgress(route, 1, 1, 1)),
        )
    }

    @Test
    fun `evenly limits points for route progress`() {
        val limit = 5

        assertEquals(
            collectPoints(*ALL_STEPS.toTypedArray()).takeEvenly(limit),
            SearchAlongRouteUtils.selectPoints(
                progress = mockkRouteProgress(ROUTE_FULL, 0, 0, 0),
                limit = limit,
            ),
        )
    }

    private companion object {

        val STEP_0_0 = generateStep(listOf(0.0 to 1.0))
        val STEP_0_1 = generateStep(listOf(1.0 to 2.0, 2.0 to 3.0))
        val STEP_0_2 = generateStep(listOf(3.0 to 4.0, 4.0 to 5.0, 5.0 to 6.0))

        val STEP_1_0 = generateStep(listOf(6.0 to 7.0))
        val STEP_1_1 = generateStep(listOf(7.0 to 8.0, 8.0 to 9.0))

        val STEP_2_0 = generateStep(listOf(9.0 to 10.0))

        val ALL_STEPS = listOf(STEP_0_0, STEP_0_1, STEP_0_2, STEP_1_0, STEP_1_1, STEP_2_0)

        val LEG_0 = createRouteLeg(steps = listOf(STEP_0_0, STEP_0_1, STEP_0_2))
        val LEG_1 = createRouteLeg(steps = listOf(STEP_1_0, STEP_1_1))
        val LEG_2 = createRouteLeg(steps = listOf(STEP_2_0))

        val ROUTE_FULL = createDirectionsRoute(legs = listOf(LEG_0, LEG_1, LEG_2))

        fun generateStep(
            points: List<Pair<Double, Double>>,
        ): LegStep {
            return createRouteStep(
                intersections = points.map {
                    Point.fromLngLat(
                        it.first,
                        it.second,
                    )
                }.map { createStepIntersection(location = it) },
            )
        }

        fun mockkRouteProgress(
            route: DirectionsRoute,
            legIndex: Int,
            stepIndex: Int,
            intersectionIndex: Int,
        ): RouteProgress {
            val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
                every { this@mockk.stepIndex } returns stepIndex
                every { this@mockk.intersectionIndex } returns intersectionIndex
            }

            val legProgress = mockk<RouteLegProgress>(relaxed = true) {
                every { currentStepProgress } returns stepProgress
                every { this@mockk.legIndex } returns legIndex
            }

            return mockk<RouteProgress>(relaxed = true) {
                every { currentLegProgress } returns legProgress
                every { this@mockk.route } returns route
            }
        }

        fun collectPoints(vararg steps: LegStep): List<Point> {
            return steps
                .mapNotNull { it.intersections() }
                .flatten()
                .map { it.location() }
                .toList()
        }
    }
}
