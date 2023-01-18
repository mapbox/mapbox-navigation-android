package com.mapbox.navigation.base.utils.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
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
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = false; " +
                            "the puck is in the very beginning",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, true, true),
                        provideBooleansProvider(false, true, true),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(true, false, false),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, false, false),
                        provideBooleansProvider(true, false, false),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(false, false, false),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the first closure of 2",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        0,
                        6,
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false; " +
                            "the puck is on the second closure of 2",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        1,
                        7,
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = true",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        0,
                        5,
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false;" +
                            "the puck has just stepped on the closure",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = true",
                        "route_closure_second_silent_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = true",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(false, false, true),
                        provideBooleansProvider(false, false, true),
                        0,
                        0,
                        false,
                    ),
                    arrayOf(
                        "route closure between silent and regular waypoints",
                        "route_closure_between_silent_and_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        0,
                        0,
                        true,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        0,
                        0,
                        true,
                    ),
                )

            private fun mockNavigationRoute(
                routeRaw: String,
                snappingIncludeClosures: BooleansProvider?,
                snappingIncludeStaticClosures: BooleansProvider?,
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
                    every { directionsRoute } returns route
                    every { id } returns "-1"
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
