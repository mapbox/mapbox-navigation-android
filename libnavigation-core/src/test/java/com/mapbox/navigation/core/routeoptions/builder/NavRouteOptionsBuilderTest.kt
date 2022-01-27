@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.routeoptions.builder

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRouteOptionsBuilderTest {

    @Test
    fun `route from current location to destination contains all coordinates`() {
        val currentLocationProvider = TestLocationProvider().apply {
            currentLocation = Point.fromLngLat(3.0, 3.0)
        }

        val options = testRouteOptionsSetup(currentLocationProvider) { builder ->
            builder
                .fromCurrentLocation()
                .toDestination(Point.fromLngLat(1.0, 1.0))
        }

        assertEquals(
            listOf(
                Point.fromLngLat(3.0, 3.0),
                Point.fromLngLat(1.0, 1.0),
            ),
            options.coordinatesList(),
        )
    }

    @Test
    fun `minimal route options contains all default parameters`() {
        val options = testRouteOptionsSetup {
            it.fromCurrentLocationToTestDestination()
        }
        assertTrue(options.steps() == true)
        // TODO: check other fields?
    }

    @Test
    fun `current position contains bearing and z level`() {
        val locationProvider = TestLocationProvider().apply {
            currentBearing = 6.8
            currentZLevel = 4
        }

        val options = testRouteOptionsSetup(locationProvider) {
            it.fromCurrentLocationToTestDestination()
        }

        assertEquals(2, options.bearingsList()?.size)
        assertEquals(6.8, options.bearingsList()!![0].angle(), 0.01)
        assertEquals(4, options.layersList()!![0])
    }

    @Test
    fun `specify properties for the destination waypoint`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .toDestination(
                    coordinate = Point.fromLngLat(1.0, 1.0),
                    name = "testName",
                    bearing = 45.0,
                    zLevel = 6
                )
        }

        assertEquals("testName", options.waypointNamesList()?.get(1))
        assertEquals(
            45.0,
            options.bearingsList()!![1].angle(),
            0.01
        )
        assertEquals(
            6,
            options.layersList()!![1]
        )
    }

    @Test
    fun `specify drop off location for an intermediate waypoint`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .addIntermediateWaypoint(
                    coordinate = Point.fromLngLat(6.0, 6.0),
                    targetCoordinate = Point.fromLngLat(7.0, 7.0),
                )
                .applyTestDestination()
        }
        assertEquals(
            listOf(null, Point.fromLngLat(7.0, 7.0), null),
            options.waypointTargetsList()
        )
    }

    @Test
    fun `specify drop off location for destination when silent waypoint is present`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .addIntermediateSilentWaypoint(
                    coordinate = Point.fromLngLat(3.0, 3.0)
                )
                .toDestination(
                    coordinate = Point.fromLngLat(5.0, 5.0),
                    targetCoordinate = Point.fromLngLat(6.0, 6.0)
                )
        }
        assertEquals(
            listOf(null, Point.fromLngLat(6.0, 6.0)),
            options.waypointTargetsList()
        )
    }

    @Test
    fun `specify custom start point, bearing and zLevel`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromStartLocation(
                    point = Point.fromLngLat(7.0, 7.0),
                    bearing = 90.0,
                    zLevel = 1
                )
                .toDestination(
                    coordinate = Point.fromLngLat(1.0, 1.0)
                )
        }

        assertEquals(
            Point.fromLngLat(7.0, 7.0),
            options.coordinatesList()[0]
        )
        assertEquals(
            1,
            options.layersList()?.get(0)
        )
        assertEquals(
            90.0,
            options.bearingsList()!![0].angle(),
            0.01
        )
    }

    @Test
    fun `add intermediate waypoint`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .addIntermediateWaypoint(
                    coordinate = Point.fromLngLat(7.0, 7.0)
                )
                .applyTestDestination()
        }
        assertEquals(3, options.coordinatesList().size)
        assertEquals(
            Point.fromLngLat(7.0, 7.0),
            options.coordinatesList()[1]
        )
    }

    @Test
    fun `add named intermediate waypoint`() {
        val options = testRouteOptionsSetup { builder ->
            builder.fromCurrentLocation()
                .addIntermediateWaypoint(
                    coordinate = Point.fromLngLat(1.0, 1.0),
                    name = "test name",
                    bearing = 45.0,
                    zLevel = 6
                )
                .applyTestDestination()
        }
        assertEquals(
            "test name",
            options.waypointNamesList()?.get(1)
        )
        assertEquals(
            45.0,
            options.bearingsList()!![1].angle(),
            0.01
        )
        assertEquals(
            6,
            options.layersList()!![1]
        )
    }

    @Test
    fun `add silent waypoint`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .addIntermediateSilentWaypoint(
                    coordinate = Point.fromLngLat(1.0, 1.0),
                    bearing = 45.0,
                    zLevel = 6
                )
                .applyTestDestination()
        }

        assertEquals(3, options.coordinatesList().size)
        assertEquals(
            Point.fromLngLat(1.0, 1.0),
            options.coordinatesList()[1]
        )
        assertEquals(
            45.0,
            options.bearingsList()!![1].angle(),
            0.01
        )
        assertEquals(
            6,
            options.layersList()!![1]
        )
        assertEquals(
            listOf(0, 2),
            options.waypointIndicesList()
        )
    }

    @Test
    fun `mix silent and named waypoints`() {
        val options = testRouteOptionsSetup { builder ->
            builder.fromCurrentLocation()
                .addIntermediateSilentWaypoint(
                    coordinate = Point.fromLngLat(1.0, 1.0)
                )
                .addIntermediateWaypoint(
                    coordinate = Point.fromLngLat(2.0, 2.0),
                    name = "test"
                )
                .applyTestDestination()
        }

        assertEquals(
            listOf(null, "test", null),
            options.waypointNamesList()
        )
    }

    @Test
    fun `driving profile with excluded toll and unpaved`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .applyTestDestination()
                .profileDriving {
                    exclude {
                        toll()
                        unpaved()
                    }
                }
        }

        assertEquals(DirectionsCriteria.PROFILE_DRIVING, options.profile())
        assertEquals(
            listOf(
                DirectionsCriteria.EXCLUDE_TOLL,
                DirectionsCriteria.EXCLUDE_UNPAVED
            ),
            options.excludeList()
        )
    }

    @Test
    fun `driving-traffic profile with excluded motorway, ferry, and cash only tolls`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocation()
                .applyTestDestination()
                .profileDrivingTraffic {
                    exclude {
                        motorway()
                        ferry()
                        cashOnlyTolls()
                    }
                }
        }

        assertEquals(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, options.profile())
        assertEquals(
            listOf(
                DirectionsCriteria.EXCLUDE_MOTORWAY,
                DirectionsCriteria.EXCLUDE_FERRY,
                DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS
            ),
            options.excludeList()
        )
    }

    @Test
    fun `walking profile with excluded cash only tolls`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileWalking {
                    exclude {
                        cashOnlyTolls()
                    }
                }
        }

        assertEquals(DirectionsCriteria.PROFILE_WALKING, options.profile())
        assertEquals(
            listOf(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS),
            options.excludeList()
        )
    }

    @Test
    fun `cycling profile with excluded ferry`() = runBlocking {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileCycling {
                    exclude {
                        ferry()
                    }
                }
        }

        assertEquals(DirectionsCriteria.PROFILE_CYCLING, options.profile())
        assertEquals(
            listOf(DirectionsCriteria.EXCLUDE_FERRY),
            options.excludeList()
        )
    }

    @Test
    fun `cycling profile with excluded cash only tolls`() = runBlocking {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileCycling {
                    exclude {
                        cashOnlyTolls()
                    }
                }
        }

        assertEquals(DirectionsCriteria.PROFILE_CYCLING, options.profile())
        assertEquals(
            listOf(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS),
            options.excludeList()
        )
    }

    @Test
    fun `driving profile with hot and hov2 includes`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileDriving {
                    include {
                        hot()
                        hov2()
                    }
                }
        }

        assertEquals(
            listOf(DirectionsCriteria.INCLUDE_HOT, DirectionsCriteria.INCLUDE_HOV2),
            options.includeList()
        )
    }

    @Test
    fun `driving-traffic profile with hov3 include`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileDrivingTraffic {
                    include {
                        hov3()
                    }
                }
        }

        assertEquals(
            listOf(DirectionsCriteria.INCLUDE_HOV3),
            options.includeList()
        )
    }

    @Test
    fun `driving profile with max width `() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileDriving {
                    maxWidth(2.0)
                }
        }

        assertEquals(2.0, options.maxWidth())
    }

    @Test
    fun `driving-traffic profile with max height `() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileDrivingTraffic {
                    maxHeight(2.0)
                }
        }

        assertEquals(2.0, options.maxHeight())
    }

    @Test
    fun `avoid walkway in walking profile`() {
        val options = testRouteOptionsSetup { builder ->
            builder
                .fromCurrentLocationToTestDestination()
                .profileWalking {
                    walkwayBias(DirectionBias.low)
                }
        }

        assertEquals(-1.0, options.walkwayBias())
    }
}

@JvmOverloads
internal fun testRouteOptionsSetup(
    locationProvider: LocationProvider = TestLocationProvider(),
    block: (NoWaypointsOptionsBuilder) -> RouteOptionsBuilderWithWaypoints
): RouteOptions {
    val builder = NavRouteOptionsBuilder(
        locationProvider
    )
    block(builder)
    return runBlocking { builder.build() }
}

internal fun NoWaypointsOptionsBuilder.fromCurrentLocationToTestDestination() = this
    .fromCurrentLocation()
    .applyTestDestination()

internal fun WaypointsInProgressBuilder.applyTestDestination() =
    toDestination(Point.fromLngLat(1.0, 1.0))

private class TestLocationProvider : LocationProvider {

    var currentLocation: Point = Point.fromLngLat(0.0, 0.0)
    var currentBearing: Double? = null
    var currentZLevel: Int? = null

    override suspend fun getCurrentLocation() = CurrentLocation(
        point = currentLocation,
        bearing = currentBearing,
        zLevel = currentZLevel
    )
}
