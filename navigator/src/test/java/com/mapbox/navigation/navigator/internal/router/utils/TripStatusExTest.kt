package com.mapbox.navigation.navigator.internal.router.utils

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.LegWaypointFactory
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.navigator.internal.utils.calculateRemainingWaypoints
import com.mapbox.navigation.navigator.internal.utils.getCurrentLegDestination
import com.mapbox.navigator.RouteState
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class TripStatusExTest {

    @RunWith(Parameterized::class)
    class CalculateRemainingWaypointsTest(
        val description: String,
        val routeWithWaypoints: NavigationRoute?,
        val nextWaypointIndex: Int,
        val routeState: RouteState,
        val expectedResult: Int,
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "null route returns 0",
                    null,
                    Int.MIN_VALUE,
                    RouteState.TRACKING,
                    0,
                ),
                arrayOf(
                    "next waypoint index normalized is normalized to default initial leg target",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                        )
                    },
                    0,
                    RouteState.TRACKING,
                    1,
                ),
                arrayOf(
                    "all waypoints are taken into account to get remaining waypoints",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.REGULAR,
                            Waypoint.SILENT,
                            Waypoint.EV_CHARGING_SERVER,
                            Waypoint.SILENT,
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                            Waypoint.SILENT,
                            Waypoint.EV_CHARGING_SERVER,
                            Waypoint.EV_CHARGING_SERVER,
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                        )
                    },
                    8,
                    RouteState.TRACKING,
                    3,
                ),
                arrayOf(
                    "charging at origin: index 0 is kept when INITIALIZED and waypoint 0 is a " +
                        "server CPOI",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.EV_CHARGING_SERVER,
                            Waypoint.REGULAR,
                        )
                    },
                    0,
                    RouteState.INITIALIZED,
                    2,
                ),
                arrayOf(
                    "charging at origin: index 0 is kept when INITIALIZED and waypoint 0 is a " +
                        "user-provided CPOI",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.EV_CHARGING_USER,
                            Waypoint.REGULAR,
                        )
                    },
                    0,
                    RouteState.INITIALIZED,
                    2,
                ),
                arrayOf(
                    "not charging at origin: waypoint 0 is regular, so index 0 is still " +
                        "normalized to 1",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                        )
                    },
                    0,
                    RouteState.INITIALIZED,
                    1,
                ),
                arrayOf(
                    "not charging at origin: state is TRACKING despite waypoint 0 being a " +
                        "CPOI, so index 0 is still normalized to 1",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.EV_CHARGING_SERVER,
                            Waypoint.REGULAR,
                        )
                    },
                    0,
                    RouteState.TRACKING,
                    1,
                ),
            )

            private fun provideMockListOfWaypoints(
                @Waypoint.Type vararg types: Int,
            ): List<Waypoint> =
                types.map { type ->
                    WaypointFactory.provideWaypoint(
                        Point.fromLngLat(0.0, 0.0),
                        "",
                        null,
                        type,
                        emptyMap(),
                    )
                }
        }

        @Test
        fun testCases() {
            val tripStatus = TripStatus(
                routeWithWaypoints,
                mockk {
                    every {
                        nextWaypointIndex
                    } returns this@CalculateRemainingWaypointsTest.nextWaypointIndex
                    every { routeState } returns this@CalculateRemainingWaypointsTest.routeState
                },
            )

            val result = tripStatus.calculateRemainingWaypoints()

            assertEquals(description, expectedResult, result)
        }
    }

    @RunWith(Parameterized::class)
    class CalculateNextLegWaypointTest(
        private val description: String,
        private val routeWithWaypoints: NavigationRoute,
        private val nextWaypointIndex: Int,
        private val expectedResult: LegWaypoint?,
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data(): List<Array<Any?>> {
                val regularWaypoint1 = WaypointFactory.provideWaypoint(
                    Point.fromLngLat(0.0, 0.0),
                    "",
                    null,
                    Waypoint.REGULAR,
                    null,
                )
                val regularWaypoint2 = WaypointFactory.provideWaypoint(
                    Point.fromLngLat(1.0, 1.0),
                    "regular waypoint",
                    Point.fromLngLat(1.1, 1.1),
                    Waypoint.REGULAR,
                    null,
                )
                val userEvWaypoint = WaypointFactory.provideWaypoint(
                    Point.fromLngLat(4.0, 4.0),
                    "user ev waypoint",
                    Point.fromLngLat(4.1, 4.1),
                    Waypoint.EV_CHARGING_USER,
                    null,
                )
                val silentWaypoint = WaypointFactory.provideWaypoint(
                    Point.fromLngLat(2.0, 2.0),
                    "",
                    null,
                    Waypoint.SILENT,
                    null,
                )
                val evWaypoint = WaypointFactory.provideWaypoint(
                    Point.fromLngLat(3.0, 3.0),
                    "ev waypoint",
                    Point.fromLngLat(3.1, 3.1),
                    Waypoint.EV_CHARGING_SERVER,
                    null,
                )
                return listOf(
                    arrayOf(
                        "no waypoints",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns emptyList()
                        },
                        0,
                        null,
                    ),
                    arrayOf(
                        "nextWaypointIndex is too big",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns listOf(
                                regularWaypoint1,
                                regularWaypoint2,
                                userEvWaypoint,
                            )
                        },
                        3,
                        null,
                    ),
                    arrayOf(
                        "no leg waypoints left",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns listOf(
                                regularWaypoint1,
                                userEvWaypoint,
                                regularWaypoint2,
                                silentWaypoint,
                            )
                        },
                        3,
                        null,
                    ),
                    arrayOf(
                        "next waypoint is regular",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns listOf(
                                regularWaypoint1,
                                userEvWaypoint,
                                silentWaypoint,
                                evWaypoint,
                                regularWaypoint2,
                            )
                        },
                        4,
                        LegWaypointFactory.createLegWaypoint(
                            Point.fromLngLat(1.0, 1.0),
                            "regular waypoint",
                            Point.fromLngLat(1.1, 1.1),
                            LegWaypoint.REGULAR,
                            null,
                        ),
                    ),
                    arrayOf(
                        "next waypoint is EV",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns listOf(
                                regularWaypoint1,
                                userEvWaypoint,
                                silentWaypoint,
                                regularWaypoint2,
                                evWaypoint,
                            )
                        },
                        4,
                        LegWaypointFactory.createLegWaypoint(
                            Point.fromLngLat(3.0, 3.0),
                            "ev waypoint",
                            Point.fromLngLat(3.1, 3.1),
                            LegWaypoint.EV_CHARGING_ADDED,
                            null,
                        ),
                    ),
                    arrayOf(
                        "next waypoint is user EV",
                        mockk<NavigationRoute> {
                            every { internalWaypoints() } returns listOf(
                                regularWaypoint1,
                                evWaypoint,
                                silentWaypoint,
                                regularWaypoint2,
                                userEvWaypoint,
                            )
                        },
                        4,
                        LegWaypointFactory.createLegWaypoint(
                            Point.fromLngLat(4.0, 4.0),
                            "user ev waypoint",
                            Point.fromLngLat(4.1, 4.1),
                            LegWaypoint.EV_CHARGING_USER_PROVIDED,
                            null,
                        ),
                    ),
                )
            }
        }

        @Test
        fun calculateNextLegWaypoint() {
            val tripStatus = TripStatus(
                null,
                mockk {
                    every {
                        nextWaypointIndex
                    } returns this@CalculateNextLegWaypointTest.nextWaypointIndex
                    every { routeState } returns RouteState.TRACKING
                },
            )

            assertEquals(expectedResult, tripStatus.getCurrentLegDestination(routeWithWaypoints))
        }
    }
}
