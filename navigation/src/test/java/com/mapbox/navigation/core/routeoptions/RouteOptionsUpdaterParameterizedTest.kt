package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.MockKAnnotations
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

@RunWith(Parameterized::class)
class RouteOptionsUpdaterParameterizedTest(
    private val coordinatesSize: Int,
    private val initWaypointNames: String,
    private val initWaypointIndices: String?,
    private val nextCoordinateIndex: Int,
    private val expectedWaypointNames: String,
    private val expectedWaypointIndices: String?,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(2, "start;finish", null, 1, ";finish", null),
            arrayOf(7, "start;finish", "0;6", 1, ";finish", "0;6"),
            arrayOf(7, "start;finish", "0;6", 3, ";finish", "0;4"),
            arrayOf(7, "start;finish", "0;6", 6, ";finish", "0;1"),
            arrayOf(3, "start;mid;finish", null, 1, ";mid;finish", null),
            arrayOf(3, "start;mid;finish", null, 2, ";finish", null),
            arrayOf(7, "start;mid;finish", "0;2;6", 2, ";mid;finish", "0;1;5"),
            arrayOf(7, "start;mid;finish", "0;2;6", 3, ";finish", "0;4"),
            arrayOf(7, "start;mid;finish", "0;2;6", 4, ";finish", "0;3"),
            arrayOf(4, "start;mid1;mid2;finish", null, 1, ";mid1;mid2;finish", null),
            arrayOf(4, "start;mid1;mid2;finish", null, 2, ";mid2;finish", null),
            arrayOf(4, "start;mid1;mid2;finish", null, 3, ";finish", null),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;4;6", 2, ";mid1;mid2;finish", "0;1;3;5"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;4;6", 3, ";mid2;finish", "0;2;4"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;4;6", 4, ";mid2;finish", "0;1;3"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;4;6", 5, ";finish", "0;2"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;4;6", 6, ";finish", "0;1"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;3;6", 3, ";mid2;finish", "0;1;4"),
            arrayOf(7, "start;mid1;mid2;finish", "0;2;3;6", 4, ";finish", "0;3"),
            arrayOf(
                7,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                null,
                1,
                ";mid1;mid2;mid3;mid4;mid5;finish",
                null,
            ),
            arrayOf(
                7,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                null,
                3,
                ";mid3;mid4;mid5;finish",
                null,
            ),
            arrayOf(
                7,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                null,
                5,
                ";mid5;finish",
                null,
            ),
            arrayOf(
                7,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6",
                1,
                ";mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6",
            ),
            arrayOf(
                7,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6",
                5,
                ";mid5;finish",
                "0;1;2",
            ),
        )
    }

    private lateinit var routeRefreshAdapter: RouteOptionsUpdater
    private lateinit var locationMatcherResult: LocationMatcherResult

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()

        routeRefreshAdapter = RouteOptionsUpdater()
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun new_options_return_modified_waypoints() {
        mockkStatic(::indexOfNextRequestedCoordinate) {
            val routeOptions = provideRouteOptionsWithCoordinates()
                .toBuilder()
                .waypointNames(initWaypointNames)
                .waypointIndices(initWaypointIndices)
                .build()
            val routeProgress: RouteProgress = mockk(relaxed = true)
            val currentLegProgress: RouteLegProgress = mockk(relaxed = true)
            every { routeProgress.currentLegProgress } returns currentLegProgress
            every { indexOfNextRequestedCoordinate(any(), any()) } returns nextCoordinateIndex
            every { routeProgress.remainingWaypoints } returns 0
            every { routeProgress.navigationRoute.internalWaypoints() } returns listOf(mockk())

            val updatedRouteOptions =
                routeRefreshAdapter.update(routeOptions, routeProgress, locationMatcherResult)
                    .let {
                        assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                        return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                    }
                    .routeOptions
            val updatedWaypointNames = updatedRouteOptions.waypointNames()
            val updatedWaypointIndices = updatedRouteOptions.waypointIndices()

            assertEquals(expectedWaypointIndices, updatedWaypointIndices)
            assertEquals(expectedWaypointNames, updatedWaypointNames)
            MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(
                routeOptions,
                updatedRouteOptions,
            )
        }
    }

    private fun mockLocation() {
        val location = mockk<Location>(relaxUnitFun = true)
        every { location.longitude } returns -122.4232
        every { location.latitude } returns 23.54423
        every { location.bearing } returns 11.0
        locationMatcherResult = mockk {
            every { enhancedLocation } returns location
            every { zLevel } returns 2
        }
    }

    private fun provideRouteOptionsWithCoordinates() =
        RouteOptions.builder()
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .coordinatesList(
                List(coordinatesSize) {
                    Point.fromLngLat(1.0, 1.0)
                },
            )
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                ),
            )
            .alternatives(true)
            .steps(true)
            .bannerInstructions(true)
            .continueStraight(true)
            .exclude(DirectionsCriteria.EXCLUDE_TOLL)
            .language("en")
            .roundaboutExits(true)
            .voiceInstructions(true)
            .build()
}
