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

    @Test
    fun no_route_options_has_route_progress_and_location_non_retryable() {
        val actual = routeRefreshAdapter.update(null, mockk(), mockk())

        assertEquals(
            PreRouterFailure("Cannot reroute as there is no active route available.", false),
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).reason,
        )
    }

    @Test
    fun no_route_options_no_route_progress_no_location_non_retryable() {
        val actual = routeRefreshAdapter.update(null, null, null)

        assertEquals(
            PreRouterFailure("Cannot reroute as there is no active route available.", false),
            (actual as RouteOptionsUpdater.RouteOptionsResult.Error).reason,
        )
    }

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
