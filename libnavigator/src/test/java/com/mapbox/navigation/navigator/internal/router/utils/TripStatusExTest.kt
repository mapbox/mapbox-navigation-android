package com.mapbox.navigation.navigator.internal.router.utils

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.navigator.internal.utils.calculateRemainingWaypoints
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
                    0,
                ),
                arrayOf(
                    "next waypoint index normalized is normalized to default initial leg target",
                    mockk<NavigationRoute> {
                        every {
                            internalWaypoints()
                        } returns provideMockListOfWaypoints(
                            Waypoint.REGULAR,
                            Waypoint.REGULAR
                        )
                    },
                    0,
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
                            Waypoint.EV_CHARGING,
                            Waypoint.SILENT,
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                            Waypoint.SILENT,
                            Waypoint.EV_CHARGING,
                            Waypoint.EV_CHARGING,
                            Waypoint.REGULAR,
                            Waypoint.REGULAR,
                        )
                    },
                    8,
                    3,
                ),
            )

            private fun provideMockListOfWaypoints(
                @Waypoint.Type vararg types: Int
            ): List<Waypoint> =
                types.map { type ->
                    WaypointFactory.provideWaypoint(
                        Point.fromLngLat(0.0, 0.0),
                        "",
                        null,
                        type
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
                }
            )

            val result = tripStatus.calculateRemainingWaypoints()

            assertEquals(description, expectedResult, result)
        }
    }
}
