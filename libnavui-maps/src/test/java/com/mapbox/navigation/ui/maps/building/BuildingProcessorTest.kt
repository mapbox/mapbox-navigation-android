package com.mapbox.navigation.ui.maps.building

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueryRenderedFeaturesCallback
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildingProcessorTest {

    @Test
    fun `map query on a regular waypoint arrival with waypoint targets`() {
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
    fun `map query on a EV waypoint arrival with waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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

        assertEquals(waypoints[1].target, result.point)
    }

    @Test
    fun `map query on waypoint arrival with some waypoint targets, all regular`() {
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
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a regular waypoint arrival with waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[2].target, result.point)
    }

    @Test
    fun `map query on a EV waypoint arrival with waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[2].target, result.point)
    }

    @Test
    fun `map query on waypoint arrival with waypoint targets, has EV after`() {
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
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.EV_CHARGING_SERVER,
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

        assertEquals(waypoints[1].target, result.point)
    }

    @Test
    fun `map query on waypoint arrival with some waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on waypoint arrival with some waypoint targets, has EV after`() {
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
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }

        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a regular waypoint arrival without waypoint targets, all regular`() {
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
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a EV waypoint arrival without waypoint targets`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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
    fun `map query on a regular waypoint arrival without waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a regular waypoint arrival without waypoint targets, has EV after`() {
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
                Waypoint.EV_CHARGING_SERVER,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.5156, 37.8989),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.5157, 37.8990),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                    Point.fromLngLat(5.5, 6.6),
                ),
            )
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a waypoint arrival without waypoint targets or coordinates, all regular`() {
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
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertNull(result.point)
    }

    @Test
    fun `map query on a EV waypoint arrival without waypoint targets or coordinates`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertEquals(waypoints[1].location, result.point)
    }

    @Test
    fun `map query on a waypoint arrival, no waypoint targets or coordinates, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertNull(result.point)
    }

    @Test
    fun `map query on a waypoint arrival without waypoint targets or coordinates, has EV after`() {
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
                Waypoint.EV_CHARGING_SERVER,
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
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnWaypoint(mockAction)

        assertNull(result.point)
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

        assertEquals(waypoints[1].target!!, result.point)
    }

    @Test
    fun `map query on final destination arrival with some waypoint targets, all regular`() {
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
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                ),
            )
        }

        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a final destination arrival with waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4146, 37.7655),
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
        }

        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(waypoints[2].target, result.point)
    }

    @Test
    fun `map query on final destination arrival with some waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                Point.fromLngLat(-122.4192, 37.7627),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
                "",
                Point.fromLngLat(-122.4183, 37.7652),
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4145, 37.7653),
                Waypoint.REGULAR,
                "",
                null,
            ),
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                ),
            )
        }

        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a final destination arrival without waypoint targets, all regular`() {
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
        )
        val mockRouteLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { legIndex } returns 0
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                ),
            )
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a final destination arrival without waypoint targets, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                ),
            )
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertEquals(Point.fromLngLat(3.3, 4.4), result.point)
    }

    @Test
    fun `map query on a final destination without waypoint targets or coordinates, all regular`() {
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
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertNull(result.point)
    }

    @Test
    fun `map query on final destination, no waypoint targets or coordinates, has EV before`() {
        val waypoints = listOf(
            provideWaypoint(
                Point.fromLngLat(-122.4192, 37.7627),
                Waypoint.REGULAR,
                "",
                null,
            ),
            provideWaypoint(
                Point.fromLngLat(-122.4182, 37.7651),
                Waypoint.EV_CHARGING_SERVER,
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
            every { legIndex } returns 1
        }
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockRouteLegProgress
            every { navigationRoute.internalWaypoints() } returns waypoints
            every { navigationRoute.routeOptions } returns createRouteOptions(emptyList())
        }
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)

        val result = BuildingProcessor.queryBuildingOnFinalDestination(mockAction)

        assertNull(result.point)
    }

    @Test
    fun `queryBuilding will query correct building layers`() = runTest {
        val callbackSlot = slot<QueryRenderedFeaturesCallback>()
        val optionsSlot = slot<RenderedQueryOptions>()
        val screenCoordinate = mockk<ScreenCoordinate>()
        val mockMap = mockk<MapboxMap>(relaxed = true) {
            every { pixelForCoordinate(any()) } returns screenCoordinate
            every {
                queryRenderedFeatures(
                    any<RenderedQueryGeometry>(),
                    any<RenderedQueryOptions>(),
                    capture(callbackSlot),
                )
            } answers {
                callbackSlot.captured.run(ExpectedFactory.createValue(listOf()))
                Cancelable {}
            }
        }
        val action = BuildingAction.QueryBuilding(
            Point.fromLngLat(-122.4145, 37.7653),
            mockMap,
        )

        BuildingProcessor.queryBuilding(action)

        verify {
            mockMap.queryRenderedFeatures(
                match { it.screenCoordinate == screenCoordinate },
                capture(optionsSlot),
                any(),
            )
        }
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
        type,
        emptyMap(),
    )
}
