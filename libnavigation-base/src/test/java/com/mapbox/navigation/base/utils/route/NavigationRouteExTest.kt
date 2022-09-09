package com.mapbox.navigation.base.utils.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.FileUtils
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private typealias BooleansProvider = () -> List<Boolean?>

class NavigationRouteExTest {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @RunWith(Parameterized::class)
    class RouteHasUnexpectedClosures(
        private val description: String,
        private val routeRaw: String,
        private val snappingIncludeClosures: BooleansProvider?,
        private val snappingIncludeStaticClosures: BooleansProvider?,
        private val expectedHasUnexpectedClosures: Boolean,
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data(): List<Array<Any?>> =
                listOf(
                    arrayOf(
                        "route without closures",
                        "multileg_route.json",
                        { null },
                        { null },
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = false",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, true, true),
                        provideBooleansProvider(false, true, true),
                        true,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(true, false, false),
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(false, false, false),
                        provideBooleansProvider(true, false, false),
                        false,
                    ),
                    arrayOf(
                        "route closure at starting waypoint, include closures = true",
                        "route_closure_start_coordinate.json",
                        provideBooleansProvider(true, false, false),
                        provideBooleansProvider(false, false, false),
                        false,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = false",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        true,
                    ),
                    arrayOf(
                        "route closure at second waypoint, include closures = true",
                        "route_closure_second_waypoint.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        false,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = false",
                        "route_closure_second_silent_waypoin.json",
                        provideBooleansProvider(true, false, true),
                        provideBooleansProvider(true, false, true),
                        true,
                    ),
                    arrayOf(
                        "route closure at second *silent* waypoint, include closures = true",
                        "route_closure_second_silent_waypoin.json",
                        provideBooleansProvider(false, true, false),
                        provideBooleansProvider(false, true, false),
                        false,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = false",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(true, true, false),
                        provideBooleansProvider(true, true, false),
                        true,
                    ),
                    arrayOf(
                        "route closure at last waypoint, include closures = true",
                        "route_closure_last_coordinate.json",
                        provideBooleansProvider(false, false, true),
                        provideBooleansProvider(false, false, true),
                        false,
                    ),
                    arrayOf(
                        "route closure between silent and regular waypoints",
                        "route_closure_between_silent_and_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
                        true,
                    ),
                    arrayOf(
                        "route closure between two regular waypoints",
                        "route_closure_between_two_regular_waypoints.json",
                        provideBooleansProvider(true, true, true),
                        provideBooleansProvider(true, true, true),
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

            private fun provideBooleansProvider(
                vararg bool: Boolean?,
            ): BooleansProvider = { bool.asList() }
        }

        @Test
        fun testCases() = runBlocking {
            val navRoute = mockNavigationRoute(
                routeRaw,
                snappingIncludeClosures,
                snappingIncludeStaticClosures,
            )

            val hasUnexpectedClosures = navRoute.hasUnexpectedClosures()

            Assert.assertEquals(
                description,
                expectedHasUnexpectedClosures,
                hasUnexpectedClosures
            )
        }
    }
}
