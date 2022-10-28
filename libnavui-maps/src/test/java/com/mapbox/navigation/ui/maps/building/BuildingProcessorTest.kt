package com.mapbox.navigation.ui.maps.building

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingProcessorTest {

    @Test
    fun `map query on waypoint arrival with waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4183, 37.7653),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[1].target!!, result.point)
    }

    @Test
    fun `map query on waypoint arrival with some waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[1].location, result.point)
    }

    @Test
    fun `map query on waypoint arrival without waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                null,
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[1].location, result.point)
    }

    @Test
    fun `map query on waypoint arrival without waypoint targets or coordinates`() {
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf()
            every { waypointTargetsList() } returns null
        }
        val mockRoute = mockk<DirectionsRoute>(relaxed = true) {
            every { routeOptions() } returns mockRouteOptions
        }
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { route } returns mockRoute
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(result.point, null)
    }

    @Test
    fun `map query on final destination arrival with waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4183, 37.7653),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(waypoints.last().target!!, result.point)
    }

    @Test
    fun `map query on final destination arrival without waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                null,
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(waypoints.last().location, result.point)
    }

    @Test
    fun `queryBuilding will query correct building layers`() = runBlockingTest {
        val callbackSlot = slot<QueryFeaturesCallback>()
        val optionsSlot = slot<RenderedQueryOptions>()
        val screenCoordinate = mockk<ScreenCoordinate>()
        val mockMap = mockk<MapboxMap>(relaxed = true) {
            every { pixelForCoordinate(any()) } returns screenCoordinate
            every {
                queryRenderedFeatures(any<ScreenCoordinate>(), any(), capture(callbackSlot))
            } answers {
                callbackSlot.captured.run(ExpectedFactory.createValue(listOf()))
            }
        }
        val action = BuildingAction.QueryBuilding(
            Point.fromLngLat(-122.4145, 37.7653),
            mockMap
        )

        BuildingProcessor.queryBuilding(action)

        verify { mockMap.queryRenderedFeatures(screenCoordinate, capture(optionsSlot), any()) }
        assertEquals(2, optionsSlot.captured.layerIds!!.size)
        assertTrue(optionsSlot.captured.layerIds!!.contains("building"))
        assertTrue(optionsSlot.captured.layerIds!!.contains("building-extrusion"))
    }

    private fun provideWaypoint(
        location: Point,
        @Waypoint.Type type: Int,
        name: String,
        target: Point?,
    ): Waypoint = WaypointFactory.provideWaypoint(
        location,
        name,
        target,
        type
    )
}
