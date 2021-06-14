package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class MapboxRouteOptionsUpdaterTest {

    private lateinit var routeRefreshAdapter: MapboxRouteOptionsUpdater
    private lateinit var location: Location

    companion object {
        private const val accessToken = "pk.1234pplffd"

        private const val DEFAULT_REROUTE_BEARING_ANGLE = 11f
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0

        private fun provideRouteOptionsWithCoordinates() =
            provideDefaultRouteOptionsBuilder()
                .coordinates(
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
                        listOf(10.0, 10.0),
                        listOf(20.0, 20.0),
                        listOf(30.0, 30.0),
                        listOf(40.0, 40.0)
                    )
                )
                .build()

        private fun provideRouteOptionsWithCoordinatesAndSnappingClosures() =
            provideRouteOptionsWithCoordinates()
                .toBuilder()
                .snappingClosures(
                    listOf(
                        true,
                        false,
                        true,
                        false
                    )
                )
                .build()

        private fun provideDefaultRouteOptionsBuilder() =
            RouteOptions.builder()
                .accessToken(accessToken)
                .baseUrl(Constants.BASE_API_URL)
                .user(Constants.MAPBOX_USER)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .annotationsList(
                    listOf(
                        DirectionsCriteria.ANNOTATION_SPEED,
                        DirectionsCriteria.ANNOTATION_CONGESTION
                    )
                )
                .coordinates(emptyList())
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .requestUuid("")
                .alternatives(true)
                .steps(true)
                .bannerInstructions(true)
                .continueStraight(true)
                .exclude(DirectionsCriteria.EXCLUDE_TOLL)
                .language("en")
                .roundaboutExits(true)
                .walkingOptions(
                    WalkingOptions.builder().walkingSpeed(5.0).build()
                )
                .voiceInstructions(true)

        private fun Any?.isNullToString(): String = if (this == null) "Null" else "NonNull"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()

        routeRefreshAdapter = MapboxRouteOptionsUpdater()
    }

    @Test
    fun new_options_return_with_null_bearings() {
        val routeOptions = provideRouteOptionsWithCoordinates()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 1
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, location)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedBearings = listOf(
            listOf(
                DEFAULT_REROUTE_BEARING_ANGLE.toDouble(),
                DEFAULT_REROUTE_BEARING_TOLERANCE
            ),
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
            routeRefreshAdapter.update(routeOptions, routeProgress, location)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions

        val expectedBearings = listOf(
            listOf(DEFAULT_REROUTE_BEARING_ANGLE.toDouble(), 10.0),
            listOf(40.0, 40.0)
        )
        val actualBearings = newRouteOptions.bearingsList()

        assertEquals(expectedBearings, actualBearings)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
    }

    @Test
    fun new_options_invalid_remaining_points() {
        val routeOptions = provideRouteOptionsWithCoordinatesAndBearings()
        val routeProgress: RouteProgress = mockk(relaxed = true) {
            every { remainingWaypoints } returns 0
        }

        val newRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, location)

        assertTrue(newRouteOptions is RouteOptionsUpdater.RouteOptionsResult.Error)
    }

    @Test
    fun no_options_on_invalid_input() {
        val invalidInput = mutableListOf<Triple<RouteOptions?, RouteProgress?, Location?>>()
        invalidInput.add(Triple(null, mockk(), mockk()))
        invalidInput.add(Triple(mockk(), null, null))
        invalidInput.add(Triple(mockk(), mockk(), null))

        invalidInput.forEach { (routeOptions, routeProgress, location) ->
            val message =
                "routeOptions is ${routeOptions.isNullToString()}; routeProgress is " +
                    "${routeProgress.isNullToString()}; location is ${location.isNullToString()}"

            assertTrue(
                message,
                routeRefreshAdapter.update(routeOptions, routeProgress, location)
                is RouteOptionsUpdater.RouteOptionsResult.Error
            )
        }
    }

    private fun mockLocation() {
        location = mockk(relaxUnitFun = true)
        every { location.longitude } returns -122.4232
        every { location.latitude } returns 23.54423
        every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
    }

    @RunWith(Parameterized::class)
    class BearingOptionsParameterized(
        val routeOptions: RouteOptions,
        val remainingWaypointsParameter: Int,
        val expectedBearings: List<List<Double>?>
    ) {

        private lateinit var routeRefreshAdapter: MapboxRouteOptionsUpdater
        private lateinit var location: Location

        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun params() = listOf(
                arrayOf(
                    provideRouteOptionsWithCoordinatesAndBearings(),
                    3,
                    listOf(
                        listOf(DEFAULT_REROUTE_BEARING_ANGLE.toDouble(), 10.0),
                        listOf(20.0, 20.0),
                        listOf(30.0, 30.0),
                        listOf(40.0, 40.0)
                    )
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates(),
                    1,
                    listOf(
                        listOf(
                            DEFAULT_REROUTE_BEARING_ANGLE.toDouble(),
                            DEFAULT_REROUTE_BEARING_TOLERANCE
                        ),
                        null
                    )
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .bearingsList(
                            listOf(
                                listOf(1.0, 2.0),
                                listOf(3.0, 4.0)
                            )
                        )
                        .build(),
                    2,
                    listOf(
                        listOf(
                            DEFAULT_REROUTE_BEARING_ANGLE.toDouble(),
                            2.0
                        ),
                        null,
                        null
                    )
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .bearingsList(
                            listOf(
                                listOf(1.0, 2.0),
                                listOf(3.0, 4.0),
                                listOf(5.0, 6.0),
                                listOf(7.0, 8.0)
                            )
                        )
                        .build(),
                    3,
                    listOf(
                        listOf(
                            DEFAULT_REROUTE_BEARING_ANGLE.toDouble(),
                            2.0
                        ),
                        listOf(3.0, 4.0),
                        listOf(5.0, 6.0),
                        listOf(7.0, 8.0)
                    )
                )
            )
        }

        @Before
        fun setup() {
            MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
            mockLocation()

            routeRefreshAdapter = MapboxRouteOptionsUpdater()
        }

        @Test
        fun bearingOptions() {
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns remainingWaypointsParameter
            }

            val newRouteOptions =
                routeRefreshAdapter.update(routeOptions, routeProgress, location)
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
            location = mockk(relaxUnitFun = true)
            every { location.longitude } returns -122.4232
            every { location.latitude } returns 23.54423
            every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
        }
    }

    @RunWith(Parameterized::class)
    class SnappingClosuresOptionsParameterized(
        val routeOptions: RouteOptions,
        val remainingWaypointsParameter: Int,
        val legIndex: Int,
        val expectedSnappingClosures: String
    ) {

        private lateinit var routeRefreshAdapter: MapboxRouteOptionsUpdater
        private lateinit var location: Location

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
                    ""
                ),
                arrayOf(
                    provideRouteOptionsWithCoordinates().toBuilder()
                        .snappingClosures(
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
                        .snappingClosures(
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

            routeRefreshAdapter = MapboxRouteOptionsUpdater()
        }

        @Test
        fun snappinClosuresOptions() {
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns remainingWaypointsParameter
                every { currentLegProgress?.legIndex } returns legIndex
            }

            val newRouteOptions =
                routeRefreshAdapter.update(routeOptions, routeProgress, location)
                    .let {
                        assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                        return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                    }
                    .routeOptions

            val actualSnappingClosures = newRouteOptions.snappingClosures()

            assertEquals(expectedSnappingClosures, actualSnappingClosures)
            MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, newRouteOptions)
        }

        private fun mockLocation() {
            location = mockk(relaxUnitFun = true)
            every { location.longitude } returns -122.4232
            every { location.latitude } returns 23.54423
            every { location.bearing } returns DEFAULT_REROUTE_BEARING_ANGLE
        }
    }
}
