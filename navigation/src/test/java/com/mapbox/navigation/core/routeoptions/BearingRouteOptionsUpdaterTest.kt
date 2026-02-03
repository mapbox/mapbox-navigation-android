package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_REROUTE_BEARING_ANGLE
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_REROUTE_BEARING_TOLERANCE
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_Z_LEVEL
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinatesAndBearings
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 *  This is a parameterized test that verifies how RouteOptionsUpdater handles the approachesList parameter during reroute operations.
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
 *
 * ✻ Crunched for 40s
 *
 * ❯ explain how BearingRouteOptionsUpdaterTest work
 *
 * ⏺ Let me read the test file to explain how it works.
 *
 * ⏺ Read 3 files (ctrl+o to expand)
 *
 * ⏺ BearingRouteOptionsUpdaterTest Explanation
 *
 *   This is a parameterized test that verifies how RouteOptionsUpdater handles the bearingsList parameter during reroute operations.
 *
 *   What is bearings?
 *
 *   The bearings parameter in the Mapbox Directions API constrains the direction of travel at each waypoint:
 *
 *   - Bearing(angle, degrees) where:
 *     - angle: The compass direction (0-360°, where 0 = North, 90 = East, etc.)
 *     - degrees: The tolerance/range (e.g., 90° = within ±90° of the angle)
 *
 *   This is used to ensure routes respect your direction of travel, especially important at:
 *   - Highway on/off ramps
 *   - One-way streets
 *   - When you want to avoid U-turns
 *
 *   What the Test Validates
 *
 *   The test verifies that RouteOptionsUpdater correctly updates the bearings list by:
 *
 *   1. Setting the origin bearing to the current vehicle heading (from location.bearing)
 *   2. Preserving the origin's tolerance from the original route
 *   3. Keeping remaining waypoint bearings from the original route
 *
 *   How Bearing Update Works
 *
 *   From RouteOptionsUpdater.kt:272-296:
 *
 *   private fun getUpdatedBearingList(
 *       remainingCoordinates: Int,
 *       nextCoordinateIndex: Int,
 *       currentAngle: Double?,           // Current vehicle heading
 *       legacyBearingList: List<Bearing?>?,
 *   ): MutableList<Bearing?> {
 *       return ArrayList<Bearing?>().also { newList ->
 *           // 1. Get tolerance from original route's first bearing (or default 90°)
 *           val originTolerance = legacyBearingList?.getOrNull(0)
 *               ?.degrees()
 *               ?: DEFAULT_REROUTE_BEARING_TOLERANCE  // 90.0
 *
 *           // 2. Create new origin bearing: current angle + original tolerance
 *           newList.add(
 *               currentAngle?.let {
 *                   Bearing.builder().angle(it).degrees(originTolerance).build()
 *               },
 *           )
 *
 *           // 3. Preserve remaining bearings from original route
 *           if (legacyBearingList != null) {
 *               for (idx in nextCoordinateIndex..legacyBearingList.lastIndex) {
 *                   newList.add(legacyBearingList[idx])
 *               }
 *           }
 *
 *           // 4. Fill with nulls if needed
 *           while (newList.size < remainingCoordinates + 1) {
 *               newList.add(null)
 *           }
 *       }
 *   }
 *
 *   Test Cases Breakdown
 *
 *   Test Case 1: Mid-route with existing bearings
 *
 *   Original bearings = [
 *       Bearing(10°, 10°),   // Coord 0
 *       Bearing(20°, 20°),   // Coord 1  ← We're here
 *       Bearing(30°, 30°),   // Coord 2
 *       Bearing(40°, 40°)    // Coord 3
 *   ]
 *   nextCoordinateIndex = 1
 *   Current bearing = 11.0°
 *
 *   Expected result = [
 *       Bearing(11°, 10°),   // New origin: current angle + original tolerance
 *       Bearing(20°, 20°),   // Preserved
 *       Bearing(30°, 30°),   // Preserved
 *       Bearing(40°, 40°)    // Preserved
 *   ]
 *   Key insight: New angle is 11° (current heading), but tolerance is 10° (from original route's first bearing)
 *
 *   Test Case 2: No bearings in original route, near destination
 *
 *   Original bearings = null
 *   nextCoordinateIndex = 3 (last waypoint)
 *   Current bearing = 11.0°
 *
 *   Expected result = [
 *       Bearing(11°, 90°),   // Current angle + default tolerance
 *       null                 // Destination (no constraint)
 *   ]
 *   Key insight: When original route had no bearings, use default tolerance of 90°
 *
 *   Test Case 3: Partial bearings with nulls
 *
 *   Original bearings = [
 *       Bearing(1°, 2°),
 *       Bearing(3°, 4°),
 *       null,               // Coord 2  ← We're here
 *       null                // Coord 3
 *   ]
 *   nextCoordinateIndex = 2
 *   Current bearing = 11.0°
 *
 *   Expected result = [
 *       Bearing(11°, 2°),   // Current angle + original tolerance (2°)
 *       null,               // Preserved from original
 *       null                // Preserved from original
 *   ]
 *   Key insight: Even if some bearings are null, the tolerance from the first bearing is still used
 *
 *   Test Case 4: Early in route
 *
 *   Original bearings = [
 *       Bearing(1°, 2°),
 *       Bearing(3°, 4°),    // Coord 1  ← We're here
 *       Bearing(5°, 6°),
 *       Bearing(7°, 8°)
 *   ]
 *   nextCoordinateIndex = 1
 *   Current bearing = 11.0°
 *
 *   Expected result = [
 *       Bearing(11°, 2°),   // Current angle + original tolerance
 *       Bearing(3°, 4°),    // Preserved
 *       Bearing(5°, 6°),    // Preserved
 *       Bearing(7°, 8°)     // Preserved
 *   ]
 *
 *   Practical Example
 *
 *   Scenario: You're driving on a highway heading North (0°) but go off-route.
 *
 *   Original Route:
 *   bearings = [
 *       Bearing(90°, 45°),   // Original: heading East ±45°
 *       Bearing(180°, 30°),  // Turn South ±30°
 *       null                 // Destination: any direction
 *   ]
 *
 *   When Rerouting:
 *   - Current heading: 0° (North)
 *   - Tolerance from original: 45°
 *
 *   New Route:
 *   bearings = [
 *       Bearing(0°, 45°),    // Start heading North ±45° (current direction)
 *       Bearing(180°, 30°),  // Still need to turn South ±30° eventually
 *       null                 // Destination unchanged
 *   ]
 *
 *   This ensures the new route:
 *   - ✅ Starts from your current direction (0° North)
 *   - ✅ Preserves the original tolerance (±45°)
 *   - ✅ Maintains destination bearing constraints
 *
 *   Why this matters: Without proper bearing handling, the router might suggest immediate U-turns or illegal maneuvers!
 */

@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class BearingRouteOptionsUpdaterTest(
    val routeOptions: RouteOptions,
    val indexNextCoordinate: Int,
    val expectedBearings: List<Bearing?>,
) {

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun params() = listOf(
            arrayOf(
                provideRouteOptionsWithCoordinatesAndBearings(),
                1,
                listOf(
                    Bearing.builder()
                        .angle(DEFAULT_REROUTE_BEARING_ANGLE)
                        .degrees(10.0)
                        .build(),
                    Bearing.builder()
                        .angle(20.0)
                        .degrees(20.0)
                        .build(),
                    Bearing.builder()
                        .angle(30.0)
                        .degrees(30.0)
                        .build(),
                    Bearing.builder()
                        .angle(40.0)
                        .degrees(40.0)
                        .build(),
                ),
            ),
            arrayOf(
                provideRouteOptionsWithCoordinates(),
                3,
                listOf(
                    Bearing.builder()
                        .angle(DEFAULT_REROUTE_BEARING_ANGLE)
                        .degrees(DEFAULT_REROUTE_BEARING_TOLERANCE)
                        .build(),
                    null,
                ),
            ),
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .bearingsList(
                        listOf(
                            Bearing.builder()
                                .angle(1.0)
                                .degrees(2.0)
                                .build(),
                            Bearing.builder()
                                .angle(3.0)
                                .degrees(4.0)
                                .build(),
                            null,
                            null,
                        ),
                    )
                    .build(),
                2,
                listOf(
                    Bearing.builder()
                        .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
                        .degrees(2.0)
                        .build(),
                    null,
                    null,
                ),
            ),
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .bearingsList(
                        listOf(
                            Bearing.builder().angle(1.0).degrees(2.0).build(),
                            Bearing.builder().angle(3.0).degrees(4.0).build(),
                            Bearing.builder().angle(5.0).degrees(6.0).build(),
                            Bearing.builder().angle(7.0).degrees(8.0).build(),
                        ),
                    )
                    .build(),
                1,
                listOf(
                    Bearing.builder()
                        .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
                        .degrees(2.0)
                        .build(),
                    Bearing.builder().angle(3.0).degrees(4.0).build(),
                    Bearing.builder().angle(5.0).degrees(6.0).build(),
                    Bearing.builder().angle(7.0).degrees(8.0).build(),
                ),
            ),
        )
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()
        mockkStatic(::indexOfNextRequestedCoordinate)

        routeRefreshAdapter = RouteOptionsUpdater()
    }

    @After
    fun cleanup() {
        unmockkStatic(::indexOfNextRequestedCoordinate)
    }

    @Test
    fun bearingOptions() {
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { indexOfNextRequestedCoordinate(any(), any()) } returns indexNextCoordinate
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val actualBearings = newRouteOptions.bearingsList()

        assertEquals(expectedBearings, actualBearings)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    private fun mockLocation() {
        val location = mockk<Location>(relaxUnitFun = true)
        every { location.longitude } returns -122.4232
        every { location.latitude } returns 23.54423
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
        locationMatcherResult = mockk {
            every { enhancedLocation } returns location
            every { zLevel } returns DEFAULT_Z_LEVEL
        }
    }
}
