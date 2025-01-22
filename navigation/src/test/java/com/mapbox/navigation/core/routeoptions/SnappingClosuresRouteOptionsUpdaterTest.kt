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
