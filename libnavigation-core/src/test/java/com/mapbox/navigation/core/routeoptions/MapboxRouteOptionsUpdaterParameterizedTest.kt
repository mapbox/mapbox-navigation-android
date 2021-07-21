package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
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

@RunWith(Parameterized::class)
class MapboxRouteOptionsUpdaterParameterizedTest(
    private val initWaypointNames: String,
    private val initWaypointIndices: String,
    private val remainingWaypoints: Int,
    private val expectedWaypointNames: String,
    private val expectedWaypointIndices: String
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf("start;finish", "0;6", 6, "start;finish", "0;6"),
            arrayOf("start;finish", "0;6", 4, "start;finish", "0;4"),
            arrayOf("start;finish", "0;6", 1, "start;finish", "0;1"),
            arrayOf("start;mid;finish", "0;2;6", 5, "start;mid;finish", "0;1;5"),
            arrayOf("start;mid;finish", "0;2;6", 4, "mid;finish", "0;4"),
            arrayOf("start;mid;finish", "0;2;6", 3, "mid;finish", "0;3"),
            arrayOf("start;mid1;mid2;finish", "0;2;4;6", 5, "start;mid1;mid2;finish", "0;1;3;5"),
            arrayOf("start;mid1;mid2;finish", "0;2;4;6", 4, "mid1;mid2;finish", "0;2;4"),
            arrayOf("start;mid1;mid2;finish", "0;2;4;6", 3, "mid1;mid2;finish", "0;1;3"),
            arrayOf("start;mid1;mid2;finish", "0;2;4;6", 2, "mid2;finish", "0;2"),
            arrayOf("start;mid1;mid2;finish", "0;2;4;6", 1, "mid2;finish", "0;1"),
            arrayOf("start;mid1;mid2;finish", "0;2;3;6", 4, "mid1;mid2;finish", "0;1;4"),
            arrayOf("start;mid1;mid2;finish", "0;2;3;6", 3, "mid2;finish", "0;3"),
            arrayOf(
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6",
                6,
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6"
            ),
            arrayOf(
                "start;mid1;mid2;mid3;mid4;mid5;finish",
                "0;1;2;3;4;5;6",
                2,
                "mid4;mid5;finish",
                "0;1;2"
            )
        )
    }

    private val accessToken = "pk.1234pplffd"

    private lateinit var routeRefreshAdapter: MapboxRouteOptionsUpdater
    private lateinit var location: Location

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        mockLocation()

        routeRefreshAdapter = MapboxRouteOptionsUpdater()
    }

    @Test
    fun new_options_return_with_all_silent_waypoints() {
        val routeOptions = provideRouteOptionsWithCoordinates()
            .toBuilder()
            .waypointNames(initWaypointNames)
            .waypointIndices(initWaypointIndices)
            .build()
        val routeProgress: RouteProgress = mockk(relaxed = true)
        val currentLegProgress: RouteLegProgress = mockk(relaxed = true)
        every { routeProgress.currentLegProgress } returns currentLegProgress
        every { routeProgress.remainingWaypoints } returns remainingWaypoints

        val updatedRouteOptions =
            routeRefreshAdapter.update(routeOptions, routeProgress, location)
                .let {
                    assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                    return@let it as RouteOptionsUpdater.RouteOptionsResult.Success
                }
                .routeOptions
        val updatedWaypointNames = updatedRouteOptions.waypointNames()
        val updatedWaypointIndices = updatedRouteOptions.waypointIndices()

        assertEquals(expectedWaypointNames, updatedWaypointNames)
        assertEquals(expectedWaypointIndices, updatedWaypointIndices)
        MapboxRouteOptionsUpdateCommonTest.checkImmutableFields(routeOptions, updatedRouteOptions)
    }

    private fun mockLocation() {
        location = mockk(relaxUnitFun = true)
        every { location.longitude } returns -122.4232
        every { location.latitude } returns 23.54423
        every { location.bearing } returns 11f
    }

    private fun provideRouteOptionsWithCoordinates() =
        RouteOptions.builder()
            .accessToken(accessToken)
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .coordinatesList(
                listOf(
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0),
                    Point.fromLngLat(1.0, 1.0)
                )
            )
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_CONGESTION
                )
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
