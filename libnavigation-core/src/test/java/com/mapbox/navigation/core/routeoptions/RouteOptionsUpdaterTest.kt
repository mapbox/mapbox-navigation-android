package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createLocation
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class RouteOptionsUpdaterTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult

    companion object {

        private const val DEFAULT_REROUTE_BEARING_ANGLE = 11f
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
        private const val DEFAULT_Z_LEVEL = 3

        private fun provideRouteOptionsWithCoordinates() =
            provideDefaultRouteOptionsBuilder()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.0, 1.0),
                        Point.fromLngLat(2.0, 2.0),
                        Point.fromLngLat(3.0, 3.0),
                        Point.fromLngLat(4.0, 4.0)
                    )
                )
                .build()

        private fun provideRouteOptionsWithCoordinatesAndBearings() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .bearingsList(
                    listOf(
                        Bearing.builder().angle(10.0).degrees(10.0).build(),
                        Bearing.builder().angle(20.0).degrees(20.0).build(),
                        Bearing.builder().angle(30.0).degrees(30.0).build(),
                        Bearing.builder().angle(40.0).degrees(40.0).build()
                    )
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
                        DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC
                    )
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
                .degrees(DEFAULT_REROUTE_BEARING_TOLERANCE)
                .build(),
            null
        )
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
                .build()
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
                // list size 1
                every { navigationRoute.routeOptions.coordinatesList() } returns listOf(mockk())
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
                is RouteOptionsUpdater.RouteOptionsResult.Error
            )
        }
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

    @RunWith(Parameterized::class)
    class BearingOptionsParameterized(
        val routeOptions: RouteOptions,
        val indexNextCoordinate: Int,
        val expectedBearings: List<Bearing?>
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
                            .build()
                    )
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    3,
                    listOf(
                        Bearing.builder()
                            .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
                            .degrees(DEFAULT_REROUTE_BEARING_TOLERANCE)
                            .build(),
                        null
                    )
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
                                null
                            )
                        )
                        .build(),
                    2,
                    listOf(
                        Bearing.builder()
                            .angle(DEFAULT_REROUTE_BEARING_ANGLE.toDouble())
                            .degrees(2.0)
                            .build(),
                        null,
                        null
                    )
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .bearingsList(
                            listOf(
                                Bearing.builder().angle(1.0).degrees(2.0).build(),
                                Bearing.builder().angle(3.0).degrees(4.0).build(),
                                Bearing.builder().angle(5.0).degrees(6.0).build(),
                                Bearing.builder().angle(7.0).degrees(8.0).build()
                            )
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
                        Bearing.builder().angle(7.0).degrees(8.0).build()
                    )
                )
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
                                false
                            )
                        )
                        .build(),
                    3,
                    0,
                    "true;false;true;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    2,
                    "true;"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingIncludeClosuresList(
                            listOf(
                                true,
                                false,
                                false,
                                false
                            )
                        )
                        .build(),
                    2,
                    1,
                    "true;false;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingIncludeClosuresList(
                            listOf(
                                true,
                                false,
                                true,
                                false
                            )
                        )
                        .build(),
                    1,
                    2,
                    "true;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingIncludeClosures(null)
                        .profile(DirectionsCriteria.PROFILE_CYCLING)
                        .build(),
                    1,
                    2,
                    null
                )
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
        val expectedSnappingStaticClosures: String?
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
                                false
                            )
                        )
                        .build(),
                    3,
                    0,
                    "true;false;true;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    2,
                    "true;"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates()
                        .toBuilder()
                        .snappingIncludeStaticClosuresList(
                            listOf(
                                true,
                                false,
                                false,
                                false
                            )
                        )
                        .build(),
                    2,
                    1,
                    "true;false;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingIncludeStaticClosuresList(
                            listOf(
                                true,
                                false,
                                true,
                                false
                            )
                        )
                        .build(),
                    1,
                    2,
                    "true;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingIncludeStaticClosuresList(null)
                        .profile(DirectionsCriteria.PROFILE_CYCLING)
                        .build(),
                    1,
                    2,
                    null
                )
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
        private val expectedApproachesList: List<String?>?
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
                            )
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
                val mockWaypoints = listOf(mockk<Waypoint>(relaxed = true))
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
                    mockLocationMatcher()
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
                every { bearing } returns 3.3f
                every { zLevel } returns 4
            }
        }
    }

    @RunWith(Parameterized::class)
    class UnrecognizedJsonPropertiesTest(
        private val description: String,
        private val routeOptions: RouteOptions,
        private val idxOfNextRequestedCoordinate: Int,
        private val expected: Map<String, JsonElement>?
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
                        "ev_add_charging_stops" to JsonPrimitive(false)
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
                        "ev_add_charging_stops" to JsonPrimitive(false),
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
                        "ev_add_charging_stops" to JsonPrimitive(false),
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
                val mockWaypoints = listOf(mockk<Waypoint>(relaxed = true))
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
                    mockLocationMatcher()
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
                every { bearing } returns 3.3f
                every { zLevel } returns 4
            }
        }
    }
}
