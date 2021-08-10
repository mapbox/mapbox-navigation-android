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
    fun `map query on waypoint arrival no point`() {
        val mockOrigin = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(mockOrigin, null)
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
    fun `map query on waypoint arrival valid point`() {
        val mockOrigin = Point.fromLngLat(-123.4567, 37.8765)
        val mockWaypoint = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(mockOrigin, mockWaypoint)
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

        assertEquals(result.point, mockWaypoint)
    }

    @Test
    fun `map query on final destination arrival no point`() {
        val mockOrigin = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(mockOrigin, null)
        }
        val mockRoute = mockk<DirectionsRoute>(relaxed = true) {
            every { routeOptions() } returns mockRouteOptions
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { route } returns mockRoute
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(result.point, null)
    }

    @Test
    fun `map query on final destination arrival valid point`() {
        val mockOrigin = Point.fromLngLat(-123.4567, 37.8765)
        val mockWaypoint = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteOptions = mockk<RouteOptions>(relaxed = true) {
            every { coordinatesList() } returns listOf(mockOrigin, mockWaypoint)
        }
        val mockRoute = mockk<DirectionsRoute>(relaxed = true) {
            every { routeOptions() } returns mockRouteOptions
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { route } returns mockRoute
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(result.point, mockWaypoint)
    }
}
