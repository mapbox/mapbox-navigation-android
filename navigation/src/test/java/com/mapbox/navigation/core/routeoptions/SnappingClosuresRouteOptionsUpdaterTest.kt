package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_REROUTE_BEARING_ANGLE
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_Z_LEVEL
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideDefaultWaypointsList
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for the `snappingIncludeClosures` parameter handling in RouteOptionsUpdater.
 *
 * ## What is snappingIncludeClosures?
 *
 * This parameter controls whether the Directions API should consider **dynamically closed roads**
 * (also called "live closures") when snapping GPS coordinates to the road network during route calculation.
 *
 * ## Dynamic Closures vs Static Closures
 *
 * There are TWO types of closure parameters:
 *
 * ### 1. `snappingIncludeClosures` (THIS TEST) - DYNAMIC/LIVE CLOSURES
 * - **Temporary, time-bound closures** reported in real-time
 * - Examples:
 *   - Traffic incidents (accidents, debris on road)
 *   - Temporary construction work
 *   - Special events (marathons, parades)
 *   - Weather-related closures
 * - Updated frequently based on live traffic data
 * - Duration: Minutes to hours, occasionally days
 *
 * ### 2. `snappingIncludeStaticClosures` (separate test) - PERMANENT CLOSURES
 * - **Long-term, permanent closures** from map data
 * - Examples:
 *   - Permanent construction projects
 *   - Road decommissioning
 *   - Seasonal closures (winter roads)
 * - Updated with map data releases
 * - Duration: Weeks to months, or permanent
 *
 * ## Use Case: Escaping from Dynamically Closed Roads
 *
 * When rerouting, you might be located on a road that's temporarily closed due to:
 * - An accident just ahead
 * - A police roadblock
 * - A parade route that just started
 *
 * To allow the router to find a valid route OUT of the closed area, the origin point
 * of the reroute must allow snapping to dynamically closed roads.
 *
 * ## Parameter Format
 *
 * Same semicolon-delimited format as static closures:
 * - Example: "true;false;false" for 3 waypoints
 *   - Waypoint 0 (origin): Include dynamic closures = true
 *   - Waypoint 1: Include dynamic closures = false
 *   - Waypoint 2 (destination): Include dynamic closures = false
 *
 * ## RouteOptionsUpdater Behavior
 *
 * During reroute, the updater:
 * 1. Always sets the FIRST element to `true` (current location = origin)
 * 2. Preserves the remaining values from the original route
 * 3. Only applies this for PROFILE_DRIVING_TRAFFIC routes
 *
 * This ensures you can navigate out of temporarily closed sections while still
 * avoiding them for the rest of the route.
 *
 * ## Real-World Example
 *
 * **Scenario**: You're driving and an accident occurs just ahead
 * 1. Road becomes dynamically closed
 * 2. You're now "on" a closed road section
 * 3. You go off-route (can't follow original path due to closure)
 * 4. Original route: `snappingIncludeClosures = "false;false;false"`
 * 5. **Without this logic**: Router can't snap your location → routing fails
 * 6. **With this logic**: Updated to `"true;false;false"`
 *    - Router snaps to your current location (even though road is closed)
 *    - Finds valid path to exit the closure area
 *    - Avoids other dynamic closures for rest of route
 *
 * ## Test Coverage
 *
 * This parameterized test validates that the snappingIncludeClosures parameter
 * is correctly updated across different scenarios:
 * - Different positions along the route
 * - Routes with and without the parameter
 * - Different routing profiles (driving vs non-driving)
 */
@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class SnappingClosuresRouteOptionsUpdaterTest(
    val routeOptions: RouteOptions,
    val remainingWaypointsParameter: Int,
    val legIndex: Int,
    val expectedSnappingClosures: String?,
) {
    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult

    companion object {

        /**
         * Parameterized test cases for snappingIncludeClosures (dynamic/live closures) handling.
         *
         * Each test case contains:
         * 1. RouteOptions: Original route with snappingIncludeClosures configuration
         * 2. remainingWaypoints: How many waypoints are left on the route
         * 3. legIndex: Current leg of the route (which segment you're on)
         * 4. expectedSnappingClosures: Expected string value after RouteOptionsUpdater processes it
         *
         * The test cases cover various scenarios to ensure dynamic closures are handled
         * correctly at different positions along a route.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(
            // Test Case 1: Beginning of route with mixed dynamic closure settings
            // Original: [true, false, true, false] - Alternating pattern
            // Position: Leg 0 (first segment), 3 waypoints remaining (at coordinate 1)
            // Coordinates: [1,1] (origin) → [2,2] → [3,3] → [4,4] (destination)
            // Expected: "true;false;true;false"
            //   - First element already "true" (matches update rule)
            //   - All waypoints still ahead, so all values preserved
            // Scenario: Starting a route where some intermediate points allow dynamic closures
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeClosuresList(
                        listOf(
                            true,
                            false,
                            true,
                            false,
                        ),
                    )
                    .build(),
                3,
                0,
                "true;false;true;false",
            ),
            // Test Case 2: Near destination with no dynamic closures specified
            // Original: null (no snappingIncludeClosures in original route)
            // Position: Leg 2 (final segment), 1 waypoint remaining (at coordinate 3)
            // Coordinates: [1,1] → [2,2] → [3,3] (current) → [4,4] (destination)
            // Expected: "true;" (origin=true + null for destination)
            // Scenario: Route didn't originally consider dynamic closures, but on reroute
            //           we need to enable it for the origin to handle any new incidents
            // Note: For driving traffic profile, origin is ALWAYS set to true on reroute
            arrayOf(
                provideRouteOptionsWithCoordinates(),
                1,
                2,
                "true;",
            ),
            // Test Case 3: Mid-route with dynamic closures disabled for most waypoints
            // Original: [true, false, false, false] - Only origin allowed closures
            // Position: Leg 1 (middle segment), 2 waypoints remaining (at coordinate 2)
            // Coordinates: [1,1] → [2,2] (current) → [3,3] → [4,4] (destination)
            // Expected: "true;false;false"
            //   - New origin: true (always set during reroute)
            //   - Remaining waypoints: [false, false] (last 2 values from original)
            // Scenario: Original route avoided dynamic closures except at start.
            //           After reroute, maintain this policy but apply to new origin.
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeClosuresList(
                        listOf(
                            true,
                            false,
                            false,
                            false,
                        ),
                    )
                    .build(),
                2,
                1,
                "true;false;false",
            ),
            // Test Case 4: Approaching destination with alternating closure settings
            // Original: [true, false, true, false] - Alternating pattern
            // Position: Leg 2 (final segment), 1 waypoint remaining (at coordinate 3)
            // Coordinates: [1,1] → [2,2] → [3,3] (current) → [4,4] (destination)
            // Expected: "true;false"
            //   - New origin: true
            //   - Destination: false (last value from original list)
            // Scenario: Complex original closure settings, but near end of route.
            //           Only the final waypoint setting is preserved.
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeClosuresList(
                        listOf(
                            true,
                            false,
                            true,
                            false,
                        ),
                    )
                    .build(),
                1,
                2,
                "true;false",
            ),
            // Test Case 5: Non-driving profile (cycling) - dynamic closures not applicable
            // Original: null (cycling profile, no closure settings)
            // Position: Leg 2, 1 waypoint remaining
            // Profile: PROFILE_CYCLING (not driving)
            // Expected: null (parameter not set at all)
            // Rationale: Dynamic road closures are primarily relevant for vehicular traffic.
            //           Cyclists may still be able to navigate through or around
            //           temporarily closed roads (sidewalks, bike paths, etc.).
            //           The snappingIncludeClosures parameter is only applied to
            //           PROFILE_DRIVING and PROFILE_DRIVING_TRAFFIC.
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeClosures(null)
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .build(),
                1,
                2,
                null,
            ),
        )
    }

    @Before
    fun setup() {
        mockLocation()

        routeRefreshAdapter = RouteOptionsUpdater()
    }

    /**
     * Main test that validates snappingIncludeClosures (dynamic closures) parameter is correctly updated.
     *
     * ## Test Flow
     * 1. Mock RouteProgress with current position (remainingWaypoints, legIndex)
     * 2. Call RouteOptionsUpdater.update() to generate new route options for reroute
     * 3. Extract the snappingIncludeClosures string from the result
     * 4. Verify it matches the expected format and values
     *
     * ## Critical Update Rule Validated
     *
     * The test ensures this transformation happens correctly:
     * - **First element (origin)**: Always set to "true" for driving traffic profiles
     * - **Remaining elements**: Preserved from the original route's corresponding waypoints
     * - **Non-driving profiles**: Parameter is not set (null)
     *
     * ## Why This Matters - Real-World Scenario
     *
     * **Example: Accident Ahead**
     * ```
     * Time: 3:00 PM - You're driving on Highway 101
     * Route: snappingIncludeClosures = "false;false;false;false"
     *        (avoiding all dynamic closures)
     *
     * Time: 3:05 PM - Accident occurs 500m ahead
     * - Highway 101 marked as dynamically closed
     * - You're now technically "on" a closed road segment
     * - Navigation detects off-route condition (can't proceed as planned)
     *
     * WITHOUT this logic:
     * - Reroute attempted with: "false;false;false"
     * - Router tries to snap your location to Highway 101
     * - But Highway 101 is closed and snapping closures = false
     * - Router CANNOT snap your position
     * - Result: "Cannot calculate route" error ❌
     *
     * WITH this logic:
     * - Reroute updates to: "true;false;false"
     * - Router snaps to your current location (even on closed Highway 101)
     * - Finds alternate path to exit the closure area
     * - Continues avoiding other dynamic closures for rest of route
     * - Result: Valid alternate route found ✅
     * ```
     *
     * ## Dynamic vs Static Closures
     *
     * This test covers **dynamic/live closures** (accidents, incidents, events).
     * See `SnappingStaticClosuresRouteOptionsUpdaterTest` for **static/permanent closures** (construction, decommissioned roads).
     *
     * Both parameters follow the same update logic but apply to different closure types.
     */
    @Test
    fun snappingClosuresOptions() {
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns remainingWaypointsParameter
            every { currentLegProgress?.legIndex } returns legIndex
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val actualSnappingClosures = newRouteOptions.snappingIncludeClosures()

        assertEquals(expectedSnappingClosures, actualSnappingClosures)
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
