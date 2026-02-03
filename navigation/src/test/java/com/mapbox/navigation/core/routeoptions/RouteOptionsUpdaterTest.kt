package com.mapbox.navigation.core.routeoptions

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.mapmatching.MapMatchingExtras
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.reroute.PreRouterFailure
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.COORDINATE_1
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.COORDINATE_2
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.COORDINATE_3
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.COORDINATE_4
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_REROUTE_BEARING_ANGLE
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.DEFAULT_Z_LEVEL
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideDefaultWaypointsList
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinatesAndArriveByDepartAt
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinatesAndBearings
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinatesAndLayers
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URL

@ExperimentalMapboxNavigationAPI
class RouteOptionsUpdaterTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult
    private lateinit var location: Location

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()

        routeRefreshAdapter = RouteOptionsUpdater()
    }

    /**
     * Tests that when the current location has no bearing information (location.bearing = null),
     * the updated route options have null bearings for all waypoints.
     *
     * Scenario: Vehicle's compass/heading is unavailable (GPS doesn't provide bearing)
     * Expected: bearingsList = [null, null] (no directional constraints)
     *
     * This ensures the router won't apply directional constraints when heading is unknown.
     */
    @Test
    fun new_options_return_with_null_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 1
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }
        val location = locationMatcherResult.enhancedLocation
        every { location.bearing } returns null

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedBearings = listOf(null, null)
        val actualBearings = newRouteOptions.bearingsList()

        assertEquals(expectedBearings, actualBearings)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests that bearing information is correctly preserved and updated during reroute.
     *
     * Scenario: Original route has bearings [10°±10°, 20°±20°, 30°±30°, 40°±40°]
     *           Currently at coordinate 3, heading 11°, only destination ahead
     *
     * Expected: bearingsList = [
     *   Bearing(11°, 10°),  // Current angle + original tolerance
     *   Bearing(40°, 40°)   // Preserved destination bearing
     * ]
     *
     * This ensures the new route respects:
     * - Current vehicle direction (11°)
     * - Original tolerance (10°) from the first waypoint
     * - Destination approach constraint (40° ± 40°)
     */
    @Test
    fun new_options_return_with_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 1
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedBearings = listOf(
            Bearing.builder()
                .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
                .degrees(10.0)
                .build(),
            Bearing.builder()
                .angle(40.0)
                .degrees(40.0)
                .build(),
        )
        val actualBearings = newRouteOptions.bearingsList()

        assertEquals(expectedBearings, actualBearings)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests that zLevel (road layer) is correctly set for reroute when original route had no layers.
     *
     * Scenario: Original route had no layer specification, but current location is on layer 3
     *           (e.g., upper deck of highway). 2 waypoints remaining.
     *
     * Expected: layersList = [3, null, null]
     *   - First element (3): Current zLevel from location matcher
     *   - Remaining elements (null): Let router decide layers for waypoints
     *
     * This ensures routes on multi-level roads (bridges, stacked highways) start from
     * the correct physical level.
     */
    @Test
    fun `new options return with current layer and nulls`() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 2
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedLayers = listOf(DEFAULT_Z_LEVEL, null, null)
        val actualLayers = newRouteOptions.layersList()

        assertEquals(expectedLayers, actualLayers)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests that zLevel (layers) is only applied for driving profiles, not for cycling/walking.
     *
     * Rationale: Multi-level roads (bridges, stacked highways) are only relevant for vehicles.
     * Cyclists and pedestrians don't use different road levels in the same way.
     *
     * Test matrix:
     * - PROFILE_CYCLING: layersList = null (no layers)
     * - PROFILE_DRIVING: layersList = [3, null, null] (includes zLevel)
     * - PROFILE_DRIVING_TRAFFIC: layersList = [3, null, null] (includes zLevel)
     * - PROFILE_WALKING: layersList = null (no layers)
     *
     * This prevents unnecessary layer information for non-driving modes.
     */
    @Test
    fun `new options return without layer for profiles other than driving`() {
        listOf(
            Pair(DirectionsCriteria.PROFILE_CYCLING, false),
            Pair(DirectionsCriteria.PROFILE_DRIVING, true),
            Pair(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, true),
            Pair(DirectionsCriteria.PROFILE_WALKING, false),
        ).forEach { (profile, result) ->
            val routeOptions = provideRouteOptionsWithCoordinates()
                .toBuilder()
                .profile(profile)
                .build()
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns 2
                every {
                    navigationRoute.internalWaypoints()
                } returns provideDefaultWaypointsList()
            }

            val newRouteOptions =
                routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                    .let {
                        assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                        return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                    }
                    .routeOptions

            val expectedLayers = if (result) {
                listOf(DEFAULT_Z_LEVEL, null, null)
            } else {
                null
            }
            val actualLayers = newRouteOptions.layersList()

            assertEquals("for $profile", expectedLayers, actualLayers)
            MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
        }
    }

    /**
     * Tests that existing layer constraints from the original route are preserved during reroute.
     *
     * Scenario: Original route specified layers [0, 1, 2, 4] for waypoints
     *           Currently at coordinate 2, on layer 3, with 2 waypoints remaining
     *
     * Expected: layersList = [3, 2, 4]
     *   - First element (3): Current zLevel
     *   - Remaining (2, 4): Preserved from original route (layers for coords 2 and 3)
     *
     * This ensures that if the original route had specific layer requirements
     * (e.g., must arrive at parking garage level 4), they are maintained.
     */
    @Test
    fun `new options return with current layer and previous layers`() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndLayers()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 2
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedLayers = listOf(DEFAULT_Z_LEVEL, 2, 4)
        val actualLayers = newRouteOptions.layersList()

        assertEquals(expectedLayers, actualLayers)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests error handling when there are no remaining waypoints (already at destination).
     *
     * Scenario: remainingWaypoints = 0 (arrived at destination)
     *
     * Expected: RouteOptionsResult.Error
     *
     * Rationale: Cannot create a valid route with no destination. This prevents
     * attempting to reroute when the trip is already complete.
     */
    @Test
    fun new_options_invalid_remaining_points() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 0
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)

        assertTrue(newRouteOptions is RouteOptionsUpdater.RouteOptionsResult.Error)
    }

    /**
     * Tests error handling when the next coordinate index cannot be determined.
     *
     * Scenario: The function indexOfNextRequestedCoordinate() returns null,
     *           meaning the next waypoint position in the route cannot be calculated.
     *
     * Expected: RouteOptionsResult.Error
     *
     * Rationale: Without knowing which coordinate to route to next, we cannot
     * build valid route options. This can happen with corrupted route data or
     * invalid waypoint configurations.
     */
    @Test
    fun index_of_next_coordinate_is_null() {
        mockkStatic(::indexOfNextRequestedCoordinate) {
            val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns 0
            }
            every { indexOfNextRequestedCoordinate(any(), any()) } returns null

            val newRouteOptions =
                routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)

            verify(exactly = 1) { indexOfNextRequestedCoordinate(any(), any()) }
            assertTrue(newRouteOptions is RouteOptionsUpdater.RouteOptionsResult.Error)
        }
    }

    /**
     * Tests error handling for various invalid input combinations.
     *
     * Validates three error scenarios:
     * 1. routeOptions = null (no original route)
     * 2. routeOptions exists, but routeProgress = null and locationMatcherResult = null
     * 3. routeOptions and routeProgress exist, but locationMatcherResult = null
     *
     * Expected: All scenarios return RouteOptionsResult.Error
     *
     * Rationale: All three inputs are required to calculate updated route options:
     * - routeOptions: Original route parameters to modify
     * - routeProgress: Current position along the route
     * - locationMatcherResult: Current location and heading information
     */
    @Test
    fun no_options_on_invalid_input() {
        val invalidInput = listOf<Triple<RouteOptions?, RouteProgress?, LocationMatcherResult?>>(
            Triple(null, mockk(), mockk()),
            Triple(mockk(), null, null),
            Triple(mockk(), mockk(), null),
        )

        invalidInput.forEach { (routeOptions, routeProgress, locationMatcherResult) ->
            val message =
                "routeOptions is ${routeOptions.isNullToString()}; routeProgress is " +
                    "${routeProgress.isNullToString()}; locationMatcherResult is " +
                    locationMatcherResult.isNullToString()

            assertTrue(
                message,
                routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                is RouteOptionsUpdater.RouteOptionsResult.Error,
            )
        }
    }

    /**
     * Tests that missing route options results in a non-retryable error.
     *
     * Scenario: routeOptions = null, but routeProgress and location are available
     *
     * Expected: PreRouterFailure with retryable = false
     *
     * Rationale: Without an original route, there's nothing to update. This is a
     * fundamental missing requirement, not a transient condition, so retrying won't help.
     * The error message: "Cannot reroute as there is no active route available."
     */
    @Test
    fun no_route_options_has_route_progress_and_location_non_retryable() {
        val actual = routeRefreshAdapter.update(null, mockk(), mockk())

        assertEquals(
            PreRouterFailure("Cannot reroute as there is no active route available.", false),
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).reason,
        )
    }

    /**
     * Tests that all null inputs results in a non-retryable error.
     *
     * Scenario: routeOptions = null, routeProgress = null, locationMatcherResult = null
     *
     * Expected: PreRouterFailure with retryable = false
     *
     * Rationale: Similar to the previous test, but validates that even when everything
     * is missing, the primary error is still "no active route available" (not missing location).
     */
    @Test
    fun no_route_options_no_route_progress_no_location_non_retryable() {
        val actual = routeRefreshAdapter.update(null, null, null)

        assertEquals(
            PreRouterFailure("Cannot reroute as there is no active route available.", false),
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).reason,
        )
    }

    /**
     * Tests that missing route progress/location results in a RETRYABLE error.
     *
     * Scenario: routeOptions exists, but routeProgress = null and locationMatcherResult = null
     *
     * Expected: PreRouterFailure with retryable = true
     *
     * Rationale: Unlike missing route options (permanent), missing progress/location might
     * be a transient condition (e.g., waiting for GPS fix). Marking it retryable allows
     * the system to try again when location becomes available.
     * Error message: "Cannot combine RouteOptions, routeProgress and locationMatcherResult cannot be null."
     */
    @Test
    fun has_route_options_no_route_progress_no_location_retryable() {
        val actual = routeRefreshAdapter.update(mockk(), null, null)

        assertEquals(
            PreRouterFailure(
                "Cannot combine RouteOptions, " +
                    "routeProgress and locationMatcherResult cannot be null.",
                true,
            ),
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).reason,
        )
    }

    /**
     * Tests that time-based constraints (arriveBy/departAt) are removed during reroute.
     *
     * Scenario: Original route had arriveBy="2021-01-01T01:01" and departAt="2021-02-02T02:02"
     *
     * Expected: Both arriveBy and departAt are set to null in updated route options
     *
     * Rationale: Time-based routing constraints (arrive by 5 PM, depart at 9 AM) are no longer
     * valid after going off-route. The original timing is invalidated by the deviation.
     * A fresh route should be calculated based on current time, not past constraints.
     *
     * Note: This is mentioned in RouteOptionsUpdater documentation:
     * "depart_at/arrive_by parameters are cleared as they are not applicable in update/re-route scenario"
     */
    @Test
    fun new_options_skip_arriveBy_departAt() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndArriveByDepartAt()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 1
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        assertNull(newRouteOptions.arriveBy())
        assertNull(newRouteOptions.departAt())
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests reroute for map-matched routes with NavigateToFinalDestination strategy.
     *
     * Context: Map-matched routes come from the Map Matching API (not Directions API),
     * typically used for replaying GPS traces or specific path requirements.
     *
     * Scenario: Map-matched route with waypoints at [1,1], [2,2], [3,3], [4,4]
     *           Current location: [11.11, 22.22]
     *           Strategy: NavigateToFinalDestination
     *
     * Expected: coordinatesList = [
     *   Point(11.11, 22.22),  // Current location
     *   COORDINATE_4           // Final destination only
     * ]
     *
     * Rationale: When rerouting a map-matched route with NavigateToFinalDestination strategy,
     * ignore all intermediate waypoints and route directly to the final destination.
     * This allows the route to be "salvaged" even though the original path is lost.
     */
    @Test
    fun `new options return origin and destination for map matched route with NavigateToFinalDestination strategy`() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val waypoints = listOf<DirectionsWaypoint>(
            mockk { every { location() } returns COORDINATE_1 },
            mockk { every { location() } returns COORDINATE_2 },
            mockk { every { location() } returns COORDINATE_3 },
            mockk { every { location() } returns COORDINATE_4 },
        )
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
            every { navigationRoute.waypoints } returns waypoints
        }
        val location = locationMatcherResult.enhancedLocation
        every { location.longitude } returns 11.11
        every { location.latitude } returns 22.22

        val newRouteOptions = routeRefreshAdapter.update(
            routeOptions,
            routeProgress,
            locationMatcherResult,
            ResponseOriginAPI.MAP_MATCHING_API,
            NavigateToFinalDestination,
        )
            .let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
            }
            .routeOptions

        val expectedCoordinates = listOf(
            Point.fromLngLat(11.11, 22.22),
            COORDINATE_4,
        )
        val actualCoordinates = newRouteOptions.coordinatesList()

        assertEquals(expectedCoordinates, actualCoordinates)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests that bearings are correctly updated for map-matched routes with NavigateToFinalDestination.
     *
     * Scenario: Map-matched route with bearings [10°±10°, 20°±20°, 30°±30°, 40°±40°]
     *           Current bearing: 11°
     *           Strategy: NavigateToFinalDestination (route to final waypoint only)
     *
     * Expected: bearingsList = [
     *   Bearing(11°, 10°),  // Current angle + original tolerance
     *   Bearing(40°, 40°)   // Final destination bearing preserved
     * ]
     *
     * Rationale: Similar to coordinates, when using NavigateToFinalDestination strategy,
     * only the origin and final destination bearings are kept. Intermediate bearings
     * are discarded along with their waypoints.
     */
    @Test
    fun `new options return with bearings for map matched route with NavigateToFinalDestination strategy`() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        val waypoints = listOf<DirectionsWaypoint>(
            mockk { every { location() } returns COORDINATE_1 },
            mockk { every { location() } returns COORDINATE_2 },
            mockk { every { location() } returns COORDINATE_3 },
            mockk { every { location() } returns COORDINATE_4 },
        )
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
            every { navigationRoute.waypoints } returns waypoints
        }

        val newRouteOptions = routeRefreshAdapter.update(
            routeOptions,
            routeProgress,
            locationMatcherResult,
            ResponseOriginAPI.MAP_MATCHING_API,
            NavigateToFinalDestination,
        )
            .let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
            }
            .routeOptions

        val expectedBearings = listOf(
            Bearing.builder()
                .angle(DEFAULT_REROUTE_BEARING_ANGLE)
                .degrees(10.0)
                .build(),
            Bearing.builder()
                .angle(40.0)
                .degrees(40.0)
                .build(),
        )
        val actualBearings = newRouteOptions.bearingsList()

        assertEquals(expectedBearings, actualBearings)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Comprehensive test for map-matched routes: validates all MapMatchingOptions are correctly
     * transformed to updated RouteOptions during reroute.
     *
     * Tests the complete flow:
     * 1. Creates MapMatchingOptions with all possible parameters
     * 2. Converts to RouteOptions via URL serialization
     * 3. Updates the options using RouteOptionsUpdater
     * 4. Verifies all parameters are correctly transformed
     *
     * Key transformations validated:
     * - Coordinates: current location + final destination only
     * - Bearings: current bearing + final destination bearing
     * - Waypoint indices: [0, 1] (origin and destination)
     * - Waypoint names: ";four" (null for origin, "four" for destination)
     * - Radiuses: ";44" (null for origin, 44.0 for destination)
     * - Layers: "3;" (current zLevel + null for destination)
     * - Unrecognized properties: cleared (empty map)
     *
     * This is an integration test ensuring the full transformation pipeline works correctly
     * for map-matched routes with NavigateToFinalDestination strategy.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `all MapMatchingOptions are mapped and updated as expected`() {
        val matchingOptions = MapMatchingOptions.Builder()
            .coordinates(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_4))
            .waypoints(listOf(0, 2, 3))
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .user("map_matched_user")
            .baseUrl("http://test.mapbox.com")
            .radiuses(listOf(null, 11.0, null, 44.0))
            .timestamps(listOf(111, 222, 333, 444))
            .annotations(
                listOf(
                    MapMatchingExtras.ANNOTATION_DURATION,
                    MapMatchingExtras.ANNOTATION_DISTANCE,
                    MapMatchingExtras.ANNOTATION_SPEED,
                    MapMatchingExtras.ANNOTATION_CONGESTION,
                    MapMatchingExtras.ANNOTATION_CONGESTION_NUMERIC,
                ),
            )
            .language("en-GB")
            .bannerInstructions(true)
            .roundaboutExits(true)
            .voiceInstructions(true)
            .tidy(true)
            .waypointNames(listOf("one", "three", "four"))
            .ignore(
                listOf(
                    MapMatchingExtras.IGNORE_ACCESS,
                    MapMatchingExtras.IGNORE_ONEWAYS,
                    MapMatchingExtras.IGNORE_RESTRICTIONS,
                ),
            )
            .openlrSpec(MapMatchingExtras.OPENLR_SPEC_HERE)
            .openlrFormat(MapMatchingExtras.OPENLR_FORMAT_TOMTOM)
            .build()

        val url = matchingOptions.toURL("***")
        val routeOptionsFromMatched = RouteOptions.fromUrl(URL(url))

        val waypoints = listOf<DirectionsWaypoint>(
            mockk { every { location() } returns COORDINATE_1 },
            mockk { every { location() } returns COORDINATE_2 },
            mockk { every { location() } returns COORDINATE_3 },
            mockk { every { location() } returns COORDINATE_4 },
        )
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
            every { navigationRoute.waypoints } returns waypoints
        }

        val newRouteOptions = routeRefreshAdapter.update(
            routeOptionsFromMatched,
            routeProgress,
            locationMatcherResult,
            ResponseOriginAPI.MAP_MATCHING_API,
            NavigateToFinalDestination,
        )
            .let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
            }
            .routeOptions

        val currentPoint = Point.fromLngLat(location.longitude, location.latitude)
        val expectedRouteOptions = routeOptionsFromMatched.toBuilder()
            .coordinatesList(listOf(currentPoint, COORDINATE_4))
            .bearings("11,90;")
            .waypointIndices("0;1")
            .waypointNames(";four")
            .radiuses(";44")
            .layers("$DEFAULT_Z_LEVEL;")
            .unrecognizedProperties(emptyMap())
            .build()

        assertEquals(expectedRouteOptions, newRouteOptions)
    }

    /**
     * Tests that zLevel is correctly handled for map-matched routes with NavigateToFinalDestination.
     *
     * Scenario: Map-matched route with NavigateToFinalDestination strategy
     *           Current zLevel: 3
     *
     * Expected: layersList = [3, null]
     *   - First element: Current zLevel (3)
     *   - Second element: null (let router decide layer for destination)
     *
     * Rationale: For map-matched routes navigating to final destination, only 2 waypoints
     * remain (current + destination), so only 2 layer values are needed.
     */
    @Test
    fun `new options return with current layer and nulls for map matched route with NavigateToFinalDestination strategy`() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val waypoints = listOf<DirectionsWaypoint>(
            mockk { every { location() } returns COORDINATE_1 },
            mockk { every { location() } returns COORDINATE_2 },
            mockk { every { location() } returns COORDINATE_3 },
            mockk { every { location() } returns COORDINATE_4 },
        )
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
            every { navigationRoute.waypoints } returns waypoints
        }

        val newRouteOptions = routeRefreshAdapter.update(
            routeOptions,
            routeProgress,
            locationMatcherResult,
            ResponseOriginAPI.MAP_MATCHING_API,
            NavigateToFinalDestination,
        )
            .let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
            }
            .routeOptions

        val expectedLayers = listOf(DEFAULT_Z_LEVEL, null)
        val actualLayers = newRouteOptions.layersList()

        assertEquals(expectedLayers, actualLayers)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests error handling for invalid responseOriginAPI parameter.
     *
     * Scenario: responseOriginAPI = "invalidResponseOriginAPI" (not DIRECTIONS_API or MAP_MATCHING_API)
     *
     * Expected: RouteOptionsResult.Error with message "Invalid responseOriginAPI = invalidResponseOriginAPI"
     *
     * Rationale: The responseOriginAPI parameter determines how route options are processed.
     * Only two valid values are supported:
     * - ResponseOriginAPI.DIRECTIONS_API: Standard Directions API routes
     * - ResponseOriginAPI.MAP_MATCHING_API: Map-matched routes
     *
     * Invalid values indicate a programming error and should fail fast.
     */
    @Test
    fun `new options invalid responseOriginAPI`() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val routeProgress: RouteProgress = mockk(relaxed = true)

        val actual = routeRefreshAdapter.update(
            routeOptions,
            routeProgress,
            locationMatcherResult,
            "invalidResponseOriginAPI",
        )

        assertEquals(
            "Invalid responseOriginAPI = invalidResponseOriginAPI",
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).error.message,
        )
    }

    /**
     * Tests that rerouting is properly rejected when RerouteDisabled strategy is used.
     *
     * Scenario: Map-matched route with RerouteDisabled strategy
     *
     * Expected: RouteOptionsResult.Error with message "Reroute disabled for the current map matched route."
     *
     * Rationale: Map-matched routes represent specific GPS traces or paths. The RerouteDisabled
     * strategy explicitly indicates that deviations should not trigger new route requests.
     * This is useful when:
     * - Replaying historical GPS traces
     * - Following a pre-defined specific path
     * - In scenarios where deviating from the exact path is acceptable without rerouting
     *
     * The updater respects this configuration and returns an error instead of attempting
     * to create new route options.
     */
    @Test
    fun `new options map matched route with RerouteDisabled strategy`() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val routeProgress: RouteProgress = mockk(relaxed = true)

        val actual = routeRefreshAdapter.update(
            routeOptions,
            routeProgress,
            locationMatcherResult,
            ResponseOriginAPI.MAP_MATCHING_API,
            RerouteDisabled,
        )

        assertEquals(
            "Reroute disabled for the current map matched route.",
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).error.message,
        )
    }

    /**
     * Tests that waypoint indices are correctly updated for map-matched routes.
     *
     * Scenario: Map-matched route with waypointIndices = [0, 3]
     *           (waypoints at coordinate index 0 and 3)
     *           Strategy: NavigateToFinalDestination
     *
     * Expected: waypointIndicesList = [0, 1]
     *   - Index 0: Current location (new origin)
     *   - Index 1: Final destination
     *
     * Rationale: For map-matched routes with NavigateToFinalDestination, the coordinate
     * list is reduced to just 2 points (current location + final destination).
     * The waypoint indices must be updated to reflect this new 2-point geometry:
     * - Original: 4 coordinates with waypoints at indices [0, 3]
     * - Updated: 2 coordinates with waypoints at indices [0, 1]
     *
     * Waypoint indices tell the router which coordinates in the list are actual
     * waypoints vs. intermediate shape points.
     */
    @Test
    fun new_options_return_with_valid_waypoints_for_map_matched_route() {
        val routeOptions = provideRouteOptionsWithCoordinates().toBuilder()
            .waypointIndicesList(listOf(0, 3))
            .build()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 2
            every { navigationRoute.internalWaypoints() } returns provideDefaultWaypointsList()
            val waypoint = mockk<DirectionsWaypoint> {
                every { location() } returns mockk {
                    every { latitude() } returns 111.111
                    every { longitude() } returns 222.222
                }
            }
            every { navigationRoute.waypoints } returns
                listOf(waypoint, waypoint, waypoint, waypoint)
        }
        val location = mockk<Location> {
            every { longitude } returns 11.11
            every { latitude } returns 22.22
            every { bearing } returns null
        }
        every { locationMatcherResult.enhancedLocation } returns location

        val newRouteOptions =
            routeRefreshAdapter.update(
                routeOptions,
                routeProgress,
                locationMatcherResult,
                ResponseOriginAPI.MAP_MATCHING_API,
                NavigateToFinalDestination,
            )
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedWaypoints = listOf(0, 1)
        val actualWaypoints = newRouteOptions.waypointIndicesList()

        assertEquals(expectedWaypoints, actualWaypoints)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    /**
     * Tests that the generated equals(), hashCode(), and toString() methods work correctly
     * for RouteOptionsResult sealed class implementations.
     *
     * Validates:
     * - RouteOptionsResult.Success: equals/hashCode/toString implementations
     * - RouteOptionsResult.Error: equals/hashCode/toString implementations
     *
     * Uses:
     * - EqualsVerifier: Ensures equals() and hashCode() follow the Java contract
     * - ToStringVerifier: Ensures toString() includes all fields and works correctly
     *
     * This is important for:
     * - Comparing results in tests
     * - Using results as map keys or in collections
     * - Debugging (readable toString output)
     */
    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            RouteOptionsUpdater.RouteOptionsResult.Success::class.java,
            RouteOptionsUpdater.RouteOptionsResult.Error::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
            ToStringVerifier.forClass(it).verify()
        }
    }

    private fun mockLocation() {
        location = mockk<Location>(relaxUnitFun = true)
        every { location.longitude } returns -122.4232
        every { location.latitude } returns 23.54423
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
        locationMatcherResult = mockk {
            every { enhancedLocation } returns location
            every { zLevel } returns DEFAULT_Z_LEVEL
        }
    }

    private companion object {
        fun Any?.isNullToString(): String = if (this == null) "Null" else "NonNull"
    }
}
