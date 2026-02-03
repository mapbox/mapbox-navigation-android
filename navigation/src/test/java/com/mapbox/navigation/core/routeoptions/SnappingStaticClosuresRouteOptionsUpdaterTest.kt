package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_REROUTE_BEARING_ANGLE
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_Z_LEVEL
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideDefaultWaypointsList
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createLocation
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for the `snappingIncludeStaticClosures` parameter handling in RouteOptionsUpdater.
 *
 * ## What is snappingIncludeStaticClosures?
 *
 * This parameter controls whether the Directions API should consider **permanently closed roads**
 * when snapping GPS coordinates to the road network during route calculation.
 *
 * ### Use Case: Escaping from Closed Roads
 *
 * When rerouting, you might be located on a road that's permanently closed (construction,
 * road closure, etc.). To allow the router to find a valid route OUT of the closed area,
 * the origin point of the reroute must allow snapping to closed roads.
 *
 * ### Parameter Format
 *
 * The parameter is a semicolon-delimited string with one boolean per waypoint:
 * - Example: "true;false;false" for 3 waypoints
 *   - Waypoint 0 (origin): Include static closures = true
 *   - Waypoint 1: Include static closures = false
 *   - Waypoint 2 (destination): Include static closures = false
 *
 * ### RouteOptionsUpdater Behavior
 *
 * During reroute, the updater:
 * 1. Always sets the FIRST element to `true` (current location = origin)
 * 2. Preserves the remaining values from the original route
 * 3. Only applies this for PROFILE_DRIVING_TRAFFIC routes
 *
 * This ensures you can navigate out of closed sections while still avoiding them
 * for the rest of the route.
 *
 * ## Test Coverage
 *
 * This parameterized test validates that the snappingIncludeStaticClosures parameter
 * is correctly updated across different scenarios:
 * - Different positions along the route
 * - Routes with and without the parameter
 * - Different routing profiles
 */
@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class SnappingStaticClosuresRouteOptionsUpdaterTest(
    val routeOptions: RouteOptions,
    val remainingWaypointsParameter: Int,
    val legIndex: Int,
    val expectedSnappingStaticClosures: String?,
) {
    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult

    companion object {

        /**
         * Parameterized test cases for snappingIncludeStaticClosures handling.
         *
         * Each test case contains:
         * 1. RouteOptions: Original route with snappingIncludeStaticClosures configuration
         * 2. remainingWaypoints: How many waypoints are left on the route
         * 3. legIndex: Current leg of the route
         * 4. expectedSnappingStaticClosures: Expected string value after update
         */
        @JvmStatic
        @Parameterized.Parameters
        fun params() = listOf(
            // Test Case 1: At beginning of route, all waypoints remain
            // Original: [true, false, true, false]
            // Position: Leg 0, 3 waypoints remaining (at coordinate 1)
            // Expected: "true;false;true;false" (first element already true, rest preserved)
            arrayOf(
                provideRouteOptionsWithCoordinates()
                    .toBuilder()
                    .snappingIncludeStaticClosuresList(
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
            // Test Case 2: Near destination, no snapping closures in original route
            // Original: null (no snapping closures specified)
            // Position: Leg 2, 1 waypoint remaining (at coordinate 3)
            // Expected: "true;" (origin=true, destination=null)
            // Note: For driving traffic profile, origin is always set to true
            arrayOf(
                provideRouteOptionsWithCoordinates(),
                1,
                2,
                "true;",
            ),
            // Test Case 3: Mid-route with snapping closures
            // Original: [true, false, false, false]
            // Position: Leg 1, 2 waypoints remaining (at coordinate 2)
            // Expected: "true;false;false" (origin=true + last 2 values from original)
            arrayOf(
                provideRouteOptionsWithCoordinates()
                    .toBuilder()
                    .snappingIncludeStaticClosuresList(
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
            // Test Case 4: Approaching destination with mixed snapping closures
            // Original: [true, false, true, false]
            // Position: Leg 2, 1 waypoint remaining (at coordinate 3)
            // Expected: "true;false" (origin=true + last value from original)
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeStaticClosuresList(
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
            // Test Case 5: Non-driving profile (cycling)
            // Original: null (cycling profile)
            // Position: Leg 2, 1 waypoint remaining
            // Expected: null (snapping closures not applied to cycling profile)
            // Rationale: Static closures are only relevant for driving profiles
            arrayOf(
                provideRouteOptionsWithCoordinates().toBuilder()
                    .snappingIncludeStaticClosuresList(null)
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
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()

        routeRefreshAdapter = RouteOptionsUpdater()
    }

    /**
     * Main test that validates snappingIncludeStaticClosures parameter is correctly updated.
     *
     * Test Flow:
     * 1. Create RouteProgress with current position (remainingWaypoints, legIndex)
     * 2. Call RouteOptionsUpdater.update() to generate new route options
     * 3. Extract the snappingIncludeStaticClosures string from result
     * 4. Verify it matches the expected format
     *
     * The test validates the critical update rule:
     * - First element (origin) is always set to "true" for driving profiles
     * - Remaining elements are preserved from the original route
     * - Non-driving profiles don't include this parameter (null)
     *
     * ## Why This Matters
     *
     * Example Scenario:
     * - You're navigating and end up on a permanently closed road (construction zone)
     * - Original route: snappingIncludeStaticClosures = "false;false;false;false"
     * - You go off-route and need to reroute
     * - Without this logic: Router can't snap your location to the closed road → routing fails
     * - With this logic: Updated to "true;false;false" → Router can snap to closed road at origin,
     *   find a valid path out, then avoid closures for the rest of the route
     *
     * This allows you to "escape" from closed roads while still avoiding them elsewhere.
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

        val actualSnappingStaticClosures = newRouteOptions.snappingIncludeStaticClosures()

        assertEquals(expectedSnappingStaticClosures, actualSnappingStaticClosures)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    private fun mockLocation() {
        val location = createLocation(
            -122.4232,
            23.54423,
            DEFAULT_REROUTE_BEARING_ANGLE,
        )
        locationMatcherResult = mockk {
            every { enhancedLocation } returns location
            every { zLevel } returns DEFAULT_Z_LEVEL
        }
    }
}
