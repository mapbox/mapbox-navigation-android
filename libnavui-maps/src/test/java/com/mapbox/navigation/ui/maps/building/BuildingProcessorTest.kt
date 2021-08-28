package com.mapbox.navigation.ui.maps.building

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildingProcessorTest {

    @Test
    fun `map query on waypoint arrival with waypoint targets`() {
        val mockOriginForWaypointTarget = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForWaypointTarget = Point.fromLngLat(-122.4183, 37.7653)
        val mockFinalForWaypointTarget = Point.fromLngLat(-122.4146, 37.7655)
        val mockOriginForCoordinates = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForCoordinates = Point.fromLngLat(-122.4182, 37.7651)
        val mockFinalForCoordinates = Point.fromLngLat(-122.4145, 37.7653)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(
                mockOriginForCoordinates,
                mockWaypointForCoordinates,
                mockFinalForCoordinates
            )
            every { waypointTargetsList() } returns listOf(
                mockOriginForWaypointTarget,
                mockWaypointForWaypointTarget,
                mockFinalForWaypointTarget
            )
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

        assertEquals(result.point, mockWaypointForWaypointTarget)
    }

    @Test
    fun `map query on waypoint arrival with some waypoint targets`() {
        val mockOriginForWaypointTarget = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForWaypointTarget = null
        val mockFinalForWaypointTarget = Point.fromLngLat(-122.4146, 37.7655)
        val mockOriginForCoordinates = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForCoordinates = Point.fromLngLat(-122.4182, 37.7651)
        val mockFinalForCoordinates = Point.fromLngLat(-122.4145, 37.7653)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(
                mockOriginForCoordinates,
                mockWaypointForCoordinates,
                mockFinalForCoordinates
            )
            every { waypointTargetsList() } returns listOf(
                mockOriginForWaypointTarget,
                mockWaypointForWaypointTarget,
                mockFinalForWaypointTarget
            )
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

        assertEquals(result.point, mockWaypointForCoordinates)
    }

    @Test
    fun `map query on waypoint arrival without waypoint targets`() {
        val mockOriginForCoordinates = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForCoordinates = Point.fromLngLat(-122.4182, 37.7651)
        val mockFinalForCoordinates = Point.fromLngLat(-122.4145, 37.7653)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(
                mockOriginForCoordinates,
                mockWaypointForCoordinates,
                mockFinalForCoordinates
            )
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

        assertEquals(result.point, mockWaypointForCoordinates)
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
        val mockOriginForWaypointTarget = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForWaypointTarget = Point.fromLngLat(-122.4183, 37.7653)
        val mockFinalForWaypointTarget = Point.fromLngLat(-122.4146, 37.7655)
        val mockOriginForCoordinates = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForCoordinates = Point.fromLngLat(-122.4182, 37.7651)
        val mockFinalForCoordinates = Point.fromLngLat(-122.4145, 37.7653)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(
                mockOriginForCoordinates,
                mockWaypointForCoordinates,
                mockFinalForCoordinates
            )
            every { waypointTargetsList() } returns listOf(
                mockOriginForWaypointTarget,
                mockWaypointForWaypointTarget,
                mockFinalForWaypointTarget
            )
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
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(result.point, mockFinalForWaypointTarget)
    }

    @Test
    fun `map query on final destination arrival without waypoint targets`() {
        val mockOriginForCoordinates = Point.fromLngLat(-122.4192, 37.7627)
        val mockWaypointForCoordinates = Point.fromLngLat(-122.4182, 37.7651)
        val mockFinalForCoordinates = Point.fromLngLat(-122.4145, 37.7653)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(
                mockOriginForCoordinates,
                mockWaypointForCoordinates,
                mockFinalForCoordinates
            )
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
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(result.point, mockFinalForCoordinates)
    }
}
