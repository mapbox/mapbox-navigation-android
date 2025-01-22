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
