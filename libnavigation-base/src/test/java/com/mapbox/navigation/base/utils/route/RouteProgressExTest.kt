package com.mapbox.navigation.base.utils.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createClosure
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private typealias BooleansProvider = () -> List<Boolean?>

class RouteProgressExTest {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @RunWith(Parameterized::class)
    class RouteHasUnexpectedUpcomingClosures(
        private val description: String,
        private val routeRaw: String,
        private val snappingIncludeClosures: BooleansProvider?,
        private val snappingIncludeStaticClosures: BooleansProvider?,
        private val unavoidableClosures: List<List<Closure>>,
        private val currentLegIndex: Int,
        private val currentGeometryLegIndex: Int,
        private val expectedHasUnexpectedUpcomingClosures: Boolean,
    ) {

        @get:Rule
        val loggerRule = LoggingFrontendTestRule()

        companion object {
            /**
             * - `route_closure_start_coordinate`: closure[0, 8], leg [0];
             * - `route_closure_second_waypoint`: closure[5, 8], leg [0] and closure[0, 8], leg[1];
             * - `route_closure_second_silent_waypoint`: closure[5, 16], leg [0];
             * - `route_closure_last_coordinate`: closure[4, 7], leg [1];
             * - `route_closure_between_silent_and_regular_waypoints`: closure[12, 13], leg [0];
             * - `route_closure_between_two_regular_waypoints`: closure[2, 3], leg [1];
             */
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data(): List<Array<Any?>> =
                listOf(
                    arrayOf(
                        "route without closures",
                        "multileg_route.json",
                        { null },
                        { null },
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = false; " +
                            "the puck is in the very beginning, all closures are expected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, true, true),
                        provideBooleansProvider(false, true, true),
                        listOf(listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = false; " +
                            "the puck is in the very beginning, all closures are unexpected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, true, true),
                        provideBooleansProvider(false, true, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(true, false, false),
                        listOf(listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(true, false, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, false, false),
                        provideBooleansProvider(true, false, false),
                        listOf(listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, false, false),
                        provideBooleansProvider(true, false, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(false, false, false),
                        listOf(listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(false, false, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false, " +
                            "all closures are expected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false, " +
                            "all closures are unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the first closure of 2, all closures are expected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
                        0,
                        6,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the first closure of 2, all closures are unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        emptyList<Closure>(),
                        0,
                        6,
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the first closure of 2, first closure is unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(), listOf(createClosure(0, 8))),
                        0,
                        6,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the first closure of 2, second closure is unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 8)), listOf()),
                        0,
                        6,
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the second closure of 2, all closures are expected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
                        1,
                        7,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the second closure of 2, all closures are unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        emptyList<Closure>(),
                        1,
                        7,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false, " +
                            "all closures are expected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 16))),
                        0,
                        5,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false, " +
                            "all closures are unexpected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        emptyList<Closure>(),
                        0,
                        5,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false;" +
                            "the puck has just stepped on the closure, all closures are expected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        listOf(listOf(createClosure(5, 16))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false;" +
                            "the puck has just stepped on the closure, all closures are unexpected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        listOf(listOf(createClosure(5, 16))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false, " +
                            "all closures are expected",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        listOf(listOf(), listOf(createClosure(4, 7))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false, " +
                            "all closures are unexpected",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        emptyList<Closure>(),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false, " +
                            "current leg = 1, all closures are unexpected",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        listOf(listOf(), listOf<Closure>()),
                        1,
                        2,
                        true,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false, " +
                            "first leg closure is unexpected, current leg = 1",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        listOf(listOf(), listOf(createClosure(4, 7))),
                        1,
                        2,
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = true, " +
                            "all closures are expected",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(false, false, true),
                        provideBooleansProvider(false, false, true),
                        listOf(listOf(), listOf(createClosure(4, 7))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = true, " +
                            "all closures are unexpected",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(false, false, true),
                        provideBooleansProvider(false, false, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure between silent and regular waypoints, " +
                            "all closures are expected",
                        "route_closure_between_silent_and_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        listOf(listOf(createClosure(12, 13))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure between silent and regular waypoints, " +
                            "all closures are unexpected",
                        "route_closure_between_silent_and_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints, all closures are expected",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        listOf(listOf(), listOf(createClosure(2, 3))),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints, all closures are unexpected",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        emptyList<Closure>(),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints, current leg = 1, " +
                            "second leg closure is unexpected",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        listOf(listOf(createClosure(2, 3)), listOf()),
                        1,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints, current leg = 1, " +
                            "first leg closure is unexpected",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        listOf(listOf(), listOf(createClosure(2, 3))),
                        1,
                        0,
                        false,
                    ),
                )

            private fun mockNavigationRoute(
                routeRaw: String,
                snappingIncludeClosures: BooleansProvider?,
                snappingIncludeStaticClosures: BooleansProvider?,
                unavoidableClosures: List<List<Closure>>,
            ): NavigationRoute {
                val route = DirectionsRoute.fromJson(
                    FileUtils.loadJsonFixture(routeRaw),
                    mockk {
                        every {
                            snappingIncludeClosuresList()
                        } answers {
                            snappingIncludeClosures?.invoke()
                        }
                        every {
                            snappingIncludeStaticClosuresList()
                        } answers {
                            snappingIncludeStaticClosures?.invoke()
                        }
                        every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE6
                    },
                    "uuid"
                )
                return mockk {
                    val navigationRoute = this
                    every { directionsRoute } returns route
                    every { id } returns "-1"
                    every { navigationRoute.unavoidableClosures } returns unavoidableClosures
                }
            }

            private fun mockRouteProgress(
                navigationRoute: NavigationRoute,
                currentLegIndex: Int,
                currentLegGeometryIndex: Int,
            ): RouteProgress = mockk {
                every { this@mockk.navigationRoute } returns navigationRoute
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns currentLegIndex
                    every { geometryIndex } returns currentLegGeometryIndex
                }
            }

            private fun provideBooleansProvider(
                vararg bool: Boolean?,
            ): BooleansProvider = { bool.asList() }
        }

        @Test
        fun testCases() = runBlocking {
            val routeProgress = mockRouteProgress(
                mockNavigationRoute(
                    routeRaw,
                    snappingIncludeClosures,
                    snappingIncludeStaticClosures,
                    unavoidableClosures,
                ),
                currentLegIndex,
                currentGeometryLegIndex,
            )

            val hasUnexpectedUpcomingClosures = routeProgress.hasUnexpectedUpcomingClosures()

            assertEquals(
                description,
                expectedHasUnexpectedUpcomingClosures,
                hasUnexpectedUpcomingClosures
            )
        }
    }
}
