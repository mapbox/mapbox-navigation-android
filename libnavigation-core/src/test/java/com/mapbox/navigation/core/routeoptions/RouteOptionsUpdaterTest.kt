package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class RouteOptionsUpdaterTest {

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
                        Point.fromLngLat(1.0, 1.0),
                        Point.fromLngLat(1.0, 1.0),
                        Point.fromLngLat(1.0, 1.0)
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

        private fun provideRouteOptionsWithCoordinatesAndSnappingClosures() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .snappingIncludeClosuresList(
                    listOf(
                        true,
                        false,
                        true,
                        false
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
    fun new_options_return_with_bearing() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 1
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
        val remainingWaypointsParameter: Int,
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
                    3,
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
                    1,
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
                    3,
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

            routeRefreshAdapter = RouteOptionsUpdater()
        }

        @Test
        fun bearingOptions() {
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns remainingWaypointsParameter
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
        val expectedSnappingClosures: String?
    ) {

        private lateinit var routeRefreshAdapter: RouteOptionsUpdater
        private lateinit var locationMatcherResult: LocationMatcherResult

        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun params() = listOf(
                arrayOf(
                    provideRouteOptionsWithCoordinatesAndSnappingClosures(),
                    3,
                    0,
                    "true;false;true;false"
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    2,
                    null
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
                    "false;false;false"
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
}
