package com.mapbox.navigation.base.internal.extensions

import com.mapbox.navigation.base.internal.route.Waypoint
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class WaypointExTest {

    @RunWith(Parameterized::class)
    class FilterTest internal constructor(
        private val waypoints: List<Waypoint>,
        private val requestedWaypointsExpected: List<Waypoint.InternalType>,
        private val legsWaypointsExpected: List<Waypoint.InternalType>,
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun data() = listOf(
                arrayOf(
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(Waypoint.InternalType.Regular, Waypoint.InternalType.Regular),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    listOf(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                ),
            )

            private fun checkWaypoints(
                expectedWaypoints: List<Waypoint.InternalType>,
                modified: List<Waypoint>,
                original: List<Waypoint>,
            ) {
                assertEquals(expectedWaypoints.size, modified.size)

                var bufferIndex = -1
                modified.forEachIndexed { index, waypoint ->
                    assertEquals(expectedWaypoints[index], waypoint.internalType)
                    assertTrue(original.contains(waypoint))
                    val idx = original.indexOf(waypoint)
                    assertTrue(idx > bufferIndex)
                    bufferIndex = idx
                }
            }
        }

        @Test
        fun testCases() {
            checkWaypoints(
                requestedWaypointsExpected,
                waypoints.filter { it.isRequestedWaypoint() },
                waypoints,
            )
            checkWaypoints(
                legsWaypointsExpected,
                waypoints.filter { it.isLegWaypoint() },
                waypoints,
            )
        }
    }

    @RunWith(Parameterized::class)
    class IndexOfNextCoordinateTest(
        private val testDescription: String,
        private val waypoints: List<Waypoint>,
        private val remainingWaypoints: Int,
        private val expectedIndex: Int?,
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "Next index: 1 for 2 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Regular,
                    ),
                    1,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    1,
                ),
                arrayOf(
                    "Next index: 2 for 3 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    1,
                    2,
                ),
                arrayOf(
                    "Next index: 3 for 4 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.Regular,
                    ),
                    1,
                    3,
                ),
                arrayOf(
                    "Next index: 1 for 2 relevant waypoints (1 is EV) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    1,
                ),
                arrayOf(
                    "Next index: 2 for 3 relevant waypoints " +
                        "(1 is EV server-provided) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    2,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints (2 is EV) and remaining waypoint 4",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    4,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 5 relevant waypoints and remaining waypoint 4",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    4,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 4 relevant waypoints " +
                        "(1 is EV server-provided) and remaining waypoint 4",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    4,
                    1,
                ),
                arrayOf(
                    "Next index: 2 for 3 relevant waypoints (2 is EV) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    2,
                ),
                arrayOf(
                    "Next index: 3 for 5 relevant waypoints and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    3,
                ),
                arrayOf(
                    "Next index: 3 for 4 relevant waypoints " +
                        "(1 is EV server-provided) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    2,
                    3,
                ),
                arrayOf(
                    "Next index: null for non-valid case - 4 relevant waypoints " +
                        "(2 is EV server-provided) and remaining waypoint 7",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    7,
                    null,
                ),
                arrayOf(
                    "Next index: 0 for 3 relevant waypoints (2 is EV) and remaining waypoint 5",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Regular,
                    ),
                    5,
                    0,
                ),
                arrayOf(
                    "Next index: 0 for 5 relevant waypoints and remaining waypoint 5",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    5,
                    0,
                ),
                arrayOf(
                    "Next index: 0 for 4 relevant waypoints " +
                        "(1 is EV server-provided) and remaining waypoint 5",
                    provideWaypoints(
                        Waypoint.InternalType.Regular,
                        Waypoint.InternalType.EvChargingServer,
                        Waypoint.InternalType.Silent,
                        Waypoint.InternalType.EvChargingUser,
                        Waypoint.InternalType.Regular,
                    ),
                    5,
                    0,
                ),
            )
        }

        @Test
        fun testCases() {
            assertEquals(
                testDescription,
                expectedIndex,
                indexOfNextRequestedCoordinate(waypoints, remainingWaypoints),
            )
        }
    }

    @RunWith(Parameterized::class)
    class IsServerAddedWaypointTest internal constructor(
        private val waypointType: Waypoint.InternalType,
        private val expected: Boolean,
    ) {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{1} for {0}")
            fun data() = listOf(
                arrayOf(Waypoint.InternalType.Silent, false),
                arrayOf(Waypoint.InternalType.EvChargingServer, true),
                arrayOf(Waypoint.InternalType.EvChargingUser, false),
                arrayOf(Waypoint.InternalType.Regular, false),
            )
        }

        @Test
        fun testCases() {
            val waypoint = provideWaypoints(waypointType).first()
            assertEquals(expected, waypoint.isServerAddedWaypoint())
        }
    }
}

private fun provideWaypoints(vararg waypointType: Waypoint.InternalType): List<Waypoint> =
    waypointType.map { mapToType ->
        mockk { every { internalType } returns mapToType }
    }
