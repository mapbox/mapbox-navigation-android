package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This is a parameterized test that verifies how RouteOptionsUpdater handles the approachesList parameter during reroute operations.
 *
 *   What is approaches?
 *
 *   The approaches parameter in the Mapbox Directions API specifies how you want to arrive at each waypoint:
 *
 *   - null - Unrestricted (approach from any direction)
 *   - DirectionsCriteria.APPROACH_UNRESTRICTED - Same as null
 *   - DirectionsCriteria.APPROACH_CURB - Arrive so destination is on your right (useful for pickups/dropoffs)
 *
 *   Each coordinate in your route can have a different approach constraint.
 *
 *   What the Test Validates
 *
 *   The test verifies that when RouteOptionsUpdater creates new route options during a reroute, it correctly updates the approaches list by:
 *
 *   1. Setting the origin approach to null (current location has no approach constraint)
 *   2. Preserving the remaining waypoint approaches from the original route
 *
 *   Test Cases
 *
 *   The test uses 3 parameterized test cases:
 *
 *   Test Case 1: Empty approaches list
 *
 *   approachesList = []
 *   nextCoordinateIndex = 2
 *   Expected result = null (no approaches)
 *   - If the original route had no approaches, the new route shouldn't have any either
 *
 *   Test Case 2: Mid-route (index 1)
 *
 *   Original approaches = [null, CURB, UNRESTRICTED, CURB]
 *   nextCoordinateIndex = 1
 *   Expected result = [null, CURB, UNRESTRICTED, CURB]
 *   - You're at coordinate 1 (still have 3 waypoints ahead)
 *   - Result: [null (new origin), CURB, UNRESTRICTED, CURB] - all remaining approaches preserved
 *
 *   Test Case 3: Near destination (index 3)
 *
 *   Original approaches = [null, CURB, UNRESTRICTED, CURB]
 *   nextCoordinateIndex = 3
 *   Expected result = [null, CURB]
 *   - You're at coordinate 3 (only final waypoint ahead)
 *   - Result: [null (new origin), CURB (final destination)] - only the last approach preserved
 *
 *   How It Works
 *
 *   Looking at RouteOptionsUpdater.kt:115-124:
 *
 *   .approachesList(
 *       let approachesList@{
 *           val approachesList = routeOptions.approachesList()
 *           if (approachesList.isNullOrEmpty()) {
 *               return@approachesList emptyList<String>()
 *           }
 *           mutableListOf<String?>() +
 *               null +                                          // Origin = null
 *               approachesList.takeLast(remainingCoordinates)   // Remaining waypoints
 *       },
 *   )
 *
 *   Algorithm:
 *   1. If original list is empty → return empty list
 *   2. Otherwise:
 *     - Create new list with null first (current location)
 *     - Add the last N approach values (where N = remaining coordinates)
 *
 *   Practical Example
 *
 *   Original Route:
 *   - Origin → Waypoint 1 (CURB) → Waypoint 2 (UNRESTRICTED) → Destination (CURB)
 *   - approaches = [null, CURB, UNRESTRICTED, CURB]
 *
 *   You go off-route at coordinate 1:
 *   - Remaining: Waypoint 1, Waypoint 2, Destination
 *   - New route should maintain: CURB, UNRESTRICTED, CURB
 *
 *   Result:
 *   New approaches = [null, CURB, UNRESTRICTED, CURB]
 *                     ↑     ↑     ↑              ↑
 *                 Current  W1     W2         Destination
 *                 location
 *
 *   This ensures that when rerouting, you still arrive at your waypoints from the correct side of the road!
 */

@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class ApproachesRouteOptionsUpdaterTest(
    private val description: String,
    private val routeOptions: RouteOptions,
    private val idxOfNextRequestedCoordinate: Int,
    private val expectedApproachesList: List<String?>?,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(
            arrayOf(
                "empty approaches list correspond to null result list",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .approachesList(emptyList())
                    .build(),
                2,
                null,
            ),
            arrayOf(
                "approaches list exist, index of next coordinate is 1, it has to contain " +
                    "all the remaining approaches",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .approachesList(
                        provideApproachesList(
                            null,
                            DirectionsCriteria.APPROACH_CURB,
                            DirectionsCriteria.APPROACH_UNRESTRICTED,
                            DirectionsCriteria.APPROACH_CURB,
                        ),
                    )
                    .build(),
                1,
                provideApproachesList(
                    null,
                    DirectionsCriteria.APPROACH_CURB,
                    DirectionsCriteria.APPROACH_UNRESTRICTED,
                    DirectionsCriteria.APPROACH_CURB,
                ),
            ),
            arrayOf(
                "approaching destination results in a list that contain null and " +
                    "the last approach value",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .approachesList(
                        provideApproachesList(
                            null,
                            DirectionsCriteria.APPROACH_CURB,
                            DirectionsCriteria.APPROACH_UNRESTRICTED,
                            DirectionsCriteria.APPROACH_CURB,
                        ),
                    )
                    .build(),
                3,
                provideApproachesList(
                    null,
                    DirectionsCriteria.APPROACH_CURB,
                ),
            ),
        )

        private fun provideApproachesList(vararg approaches: String?): List<String?> =
            approaches.toList()
    }

    @Before
    fun setup() {
        routeOptionsUpdater = RouteOptionsUpdater()
    }

    @Test
    fun testCases() {
        mockkStatic(::indexOfNextRequestedCoordinate) {
            val mockRemainingWaypoints = -1
            val mockWaypoints = listOf(mockk<Waypoint>())
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns mockRemainingWaypoints
                every { navigationRoute.internalWaypoints() } returns mockWaypoints
            }
            every {
                indexOfNextRequestedCoordinate(mockWaypoints, mockRemainingWaypoints)
            } returns idxOfNextRequestedCoordinate

            val updatedRouteOptions = routeOptionsUpdater.update(
                routeOptions,
                routeProgress,
                mockLocationMatcher(),
            ).let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                it as RouteOptionsUpdater.RouteOptionsResult.Success
            }.routeOptions

            assertEquals(expectedApproachesList, updatedRouteOptions.approachesList())
        }
    }

    private fun mockLocationMatcher(): LocationMatcherResult = mockk {
        every { enhancedLocation } returns mockk {
            every { latitude } returns 1.1
            every { longitude } returns 2.2
            every { bearing } returns 3.3
            every { zLevel } returns 4
        }
    }
}
