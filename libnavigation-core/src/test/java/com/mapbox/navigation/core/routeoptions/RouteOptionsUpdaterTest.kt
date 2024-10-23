package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.mapmatching.MapMatchingExtras
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.reroute.PreRouterFailure
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createLocation
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@ExperimentalMapboxNavigationAPI
class RouteOptionsUpdaterTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult
    private lateinit var location: Location

    companion object {

        private const val DEFAULT_REROUTE_BEARING_ANGLE = 11.0
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
        private const val DEFAULT_Z_LEVEL = 3

        private val COORDINATE_1 = Point.fromLngLat(1.0, 1.0)
        private val COORDINATE_2 = Point.fromLngLat(2.0, 2.0)
        private val COORDINATE_3 = Point.fromLngLat(3.0, 3.0)
        private val COORDINATE_4 = Point.fromLngLat(4.0, 4.0)

        private fun provideRouteOptionsWithCoordinates() =
            provideDefaultRouteOptionsBuilder()
                .coordinatesList(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_4))
                .build()

        private fun provideRouteOptionsWithCoordinatesAndBearings() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .bearingsList(
                    listOf(
                        Bearing.builder().angle(10.0).degrees(10.0).build(),
                        Bearing.builder().angle(20.0).degrees(20.0).build(),
                        Bearing.builder().angle(30.0).degrees(30.0).build(),
                        Bearing.builder().angle(40.0).degrees(40.0).build(),
                    ),
                )
                .build()

        private fun provideRouteOptionsWithCoordinatesAndArriveByDepartAt() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .arriveBy("2021-01-01'T'01:01")
                .departAt("2021-02-02'T'02:02")
                .build()

        private fun provideRouteOptionsWithCoordinatesAndLayers() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .layersList(listOf(0, 1, 2, 4))
                .build()

        private fun provideDefaultRouteOptionsBuilder() =
            RouteOptions.builder()
                .baseUrl(Constants.BASE_API_URL)
                .user(Constants.MAPBOX_USER)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .annotationsList(
                    listOf(
                        DirectionsCriteria.ANNOTATION_SPEED,
                        DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                    ),
                )
                .coordinatesList(emptyList())
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .alternatives(true)
                .steps(true)
                .bannerInstructions(true)
                .continueStraight(true)
                .exclude(DirectionsCriteria.EXCLUDE_TOLL)
                .language("en")
                .roundaboutExits(true)
                .voiceInstructions(true)

        private fun provideDefaultWaypointsList(): List<Waypoint> =
            listOf(
                WaypointFactory.provideWaypoint(
                    Point.fromLngLat(1.0, 1.0),
                    "",
                    null,
                    Waypoint.REGULAR,
                    emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    Point.fromLngLat(2.0, 2.0),
                    "",
                    null,
                    Waypoint.REGULAR,
                    emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    Point.fromLngLat(3.0, 3.0),
                    "",
                    null,
                    Waypoint.REGULAR,
                    emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    Point.fromLngLat(4.0, 4.0),
                    "",
                    null,
                    Waypoint.REGULAR,
                    emptyMap(),
                ),
            )

        private fun Any?.isNullToString(): String = if (this == null) "Null" else "NonNull"

        private fun mockWaypoint(
            location: Point,
            @Waypoint.Type type: Int,
            name: String,
            target: Point?,
        ): Waypoint = mockk {
            every { this@mockk.location } returns location
            every { this@mockk.type } returns type
            every { this@mockk.name } returns name
            every { this@mockk.target } returns target
        }
    }

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

    @RunWith(Parameterized::class)
    class BearingOptionsParameterized(
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
                            .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
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
                            .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
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

    @RunWith(Parameterized::class)
    class SnappingClosuresOptionsParameterized(
        val routeOptions: RouteOptions,
        val remainingWaypointsParameter: Int,
        val legIndex: Int,
        val expectedSnappingClosures: String?,
    ) {

        private lateinit var routeRefreshAdapter: RouteOptionsUpdater
        private lateinit var locationMatcherResult: LocationMatcherResult

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun params() = listOf(
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
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    2,
                    "true;",
                ),
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

    @RunWith(Parameterized::class)
    class SnappingStaticClosuresOptionsParameterized(
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

            @JvmStatic
            @Parameterized.Parameters
            fun params() = listOf(
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
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    2,
                    "true;",
                ),
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

    @RunWith(Parameterized::class)
    class ApproachesTestParameterized(
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
                    "approaches list exist, index of next coordinate is 1, it has to become " +
                        "null in the result list",
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
                        DirectionsCriteria.APPROACH_UNRESTRICTED,
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

    @RunWith(Parameterized::class)
    class UnrecognizedJsonPropertiesTest(
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

        private fun mockLocationMatcher(): LocationMatcherResult = mockk {
            every { enhancedLocation } returns mockk {
                every { latitude } returns 1.1
                every { longitude } returns 2.2
                every { bearing } returns 3.3
                every { zLevel } returns 4
            }
        }
    }
}
