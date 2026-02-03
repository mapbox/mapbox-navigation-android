package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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
 * Parameterized test suite verifying how [RouteOptionsUpdater] handles unrecognized JSON properties
 * during route updates, with special focus on Electric Vehicle (EV) charging station data.
 *
 * ## Overview
 * When rerouting occurs, the RouteOptions need to be updated to reflect the current navigation state.
 * This includes adjusting unrecognized JSON properties (custom properties not part of the standard
 * RouteOptions API) that represent per-waypoint data, such as EV charging station metadata.
 *
 * ## Waypoint Charging Properties Format
 * EV charging properties use semicolon-separated values where each segment corresponds to a waypoint:
 * - `waypoints.charging_station_id`: Station identifiers (e.g., ";2;;3" = [empty, "2", empty, "3"])
 * - `waypoints.charging_station_power`: Power ratings in watts (e.g., ";2000;;3000")
 * - `waypoints.charging_station_current_type`: Current types (e.g., ";ac;;dc")
 *
 * Empty segments (represented by consecutive semicolons) indicate waypoints without charging data.
 *
 * ## Step-by-Step Verification Logic
 *
 * ### Step 1: Detect EV Route
 * The updater checks if the route is an EV route by looking for the "engine" property:
 * - **EV route**: `unrecognizedJsonProperties` contains `"engine" to JsonPrimitive("electric")`
 * - **Non-EV route**: No "engine" property present
 *
 * ### Step 2: Determine Waypoint Index
 * The `idxOfNextRequestedCoordinate` indicates which waypoint is next in the route:
 * - Index 1: First coordinate (origin) - no waypoints have been passed yet
 * - Index 2: Second coordinate - first waypoint has been passed
 * - Index 3+: Subsequent coordinates - multiple waypoints may have been passed
 *
 * ### Step 3: Apply Trimming Rules
 * The updater applies different rules based on route type and waypoint index:
 *
 * #### Rule A: Non-EV Routes
 * **Action**: Preserve all waypoints.charging* properties unchanged
 * **Reason**: Non-EV routes don't need charging data adjustments
 * **Example**:
 * ```
 * Input:  "waypoints.charging_station_id" = ";;2;3"
 * Output: "waypoints.charging_station_id" = ";;2;3" (unchanged)
 * ```
 *
 * #### Rule B: EV Routes at First Coordinate (index = 1)
 * **Action**: Preserve all waypoints.charging* properties unchanged
 * **Reason**: No waypoints have been passed yet, so all data is still relevant
 * **Example**:
 * ```
 * Input:  "waypoints.charging_station_id" = ";;2;3"
 * Output: "waypoints.charging_station_id" = ";;2;3" (unchanged)
 * ```
 *
 * #### Rule C: EV Routes at Middle Coordinates (index = 2)
 * **Action**: Trim the first (index - 1) segments from each waypoints.charging* property
 * **Reason**: Passed waypoints are no longer relevant for navigation
 * **Example**:
 * ```
 * Input:  "waypoints.charging_station_id" = ";2;;3"  (4 waypoints: [empty, "2", empty, "3"])
 * Index:  2 (skip first 2 waypoints)
 * Output: "waypoints.charging_station_id" = ";;3"    (2 waypoints: [empty, "3"])
 * ```
 *
 * #### Rule D: EV Routes at Last Coordinate (index = 3+)
 * **Action**: Trim all passed waypoints, keeping only remaining segments
 * **Reason**: Only future charging stations matter for remaining route
 * **Example**:
 * ```
 * Input:  "waypoints.charging_station_id" = ";2;;3"  (4 waypoints: [empty, "2", empty, "3"])
 * Index:  3 (skip first 3 waypoints)
 * Output: "waypoints.charging_station_id" = ";3"     (1 waypoint: ["3"])
 * ```
 *
 * ### Step 4: Preserve Other Properties
 * All other unrecognized properties (not starting with "waypoints.charging") remain unchanged:
 * - `"aaa" to JsonPrimitive("bbb")` is preserved in all test cases
 * - The "engine" property is preserved for EV routes
 *
 * ## Test Scenarios
 * The parameterized tests verify:
 * 1. **Null handling**: null properties remain null
 * 2. **Empty handling**: empty maps remain empty
 * 3. **Non-EV preservation**: Charging data unchanged for non-EV routes
 * 4. **EV trimming**: Charging data correctly trimmed for EV routes
 * 5. **First coordinate**: No trimming at route start
 * 6. **Last coordinate**: Proper trimming at route end
 *
 * @param description Human-readable test case description
 * @param routeOptions Input RouteOptions with unrecognized properties
 * @param idxOfNextRequestedCoordinate Index of the next waypoint to navigate to
 * @param expected Expected unrecognized properties after update
 */
@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class UnrecognizedRouteOptionsUpdaterTest(
    private val description: String,
    private val routeOptions: RouteOptions,
    private val idxOfNextRequestedCoordinate: Int,
    private val expected: Map<String, JsonElement>?,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(
            arrayOf(
                "null unrecognized properties to null",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(null)
                    .build(),
                2,
                null,
            ),
            arrayOf(
                "empty unrecognized properties to empty",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(emptyMap())
                    .build(),
                2,
                emptyMap<String, JsonElement>(),
            ),
            arrayOf(
                "ev data present for non ev route should not be changed",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";;ac;dc"),
                        ),
                    )
                    .build(),
                2,
                mapOf(
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;ac;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should be changed",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";2;;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";2000;;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";ac;;dc"),
                        ),
                    )
                    .build(),
                2,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should not be changed for the first coordinate",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";;ac;dc"),
                        ),
                    )
                    .build(),
                1,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;ac;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should be changed for the last coordinate",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";2;;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";2000;;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";ac;;dc"),
                        ),
                    )
                    .build(),
                3,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";dc"),
                ),
            ),
        )
    }

    @Before
    fun setup() {
        routeOptionsUpdater = RouteOptionsUpdater()
    }

    @Test
    fun unrecognizedJsonProperties() {
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

            assertEquals(expected, updatedRouteOptions.unrecognizedJsonProperties)
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
