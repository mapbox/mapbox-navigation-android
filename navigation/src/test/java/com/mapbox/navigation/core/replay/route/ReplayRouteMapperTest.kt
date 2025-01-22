package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.testutil.replay.measureSpeedDistances
import com.mapbox.navigation.core.testutil.replay.removeAccelerationAndBrakingSpeedUpdates
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReplayRouteMapperTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val replayRouteMapper = ReplayRouteMapper()

    @Test
    fun `should have polyline 6 option for geometry replay`() {
        val directionRoute: DirectionsRoute = mockk {
            every { routeOptions() } returns mockk {
                every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE6
                every { geometry() } returns ""
            }
        }

        val replayEvent = replayRouteMapper.mapDirectionsRouteGeometry(directionRoute)

        assertTrue(replayEvent.isEmpty())
    }

    @Test
    fun `should map common location`() {
        val location: Location = mockk {
            every { source } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { horizontalAccuracy } returns 11.0
            every { bearing } returns 12.0
            every { speed } returns 2.0
            every { altitude } returns 25.0
        }

        val replayEvent = ReplayRouteMapper.mapToUpdateLocation(0.1, location)

        val locationUpdate = replayEvent as ReplayEventUpdateLocation
        assertEquals(0.1, locationUpdate.eventTimestamp, 0.001)
        assertEquals(-122.392624, locationUpdate.location.lat, 0.000001)
        assertEquals(37.764107, locationUpdate.location.lon, 0.000001)
        assertEquals(11.0, locationUpdate.location.accuracyHorizontal)
        assertEquals(12.0, locationUpdate.location.bearing)
        assertEquals(2.0, locationUpdate.location.speed)
        assertEquals(25.0, locationUpdate.location.altitude)
    }

    @Test
    fun `should map android location with optional`() {
        val location: Location = mockk {
            every { source } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { horizontalAccuracy } returns null
            every { bearing } returns null
            every { speed } returns null
            every { altitude } returns null
        }

        val replayEvent = ReplayRouteMapper.mapToUpdateLocation(0.1, location)

        val locationUpdate = replayEvent as ReplayEventUpdateLocation
        assertEquals(0.1, locationUpdate.eventTimestamp, 0.001)
        assertEquals(-122.392624, locationUpdate.location.lat, 0.000001)
        assertEquals(37.764107, locationUpdate.location.lon, 0.000001)
        assertNull(locationUpdate.location.accuracyHorizontal)
        assertNull(locationUpdate.location.bearing)
        assertNull(locationUpdate.location.speed)
        assertNull(locationUpdate.location.altitude)
    }

    @Test
    fun `mapRouteLegAnnotation give actionable error mess when map route leg annotations fails`() {
        val routeLegWithoutDistanceAnnotation: RouteLeg =
            RouteLeg.fromJson(resourceAsString("route_leg_without_distance_annotation_test.txt"))

        val failureMessage = try {
            replayRouteMapper.mapRouteLegAnnotation(routeLegWithoutDistanceAnnotation)
            ""
        } catch (e: Throwable) {
            e.message
        }

        assertEquals(
            "mapRouteLegAnnotation only works when there are speed and distance profiles",
            "Directions request should include annotations DirectionsCriteria.ANNOTATION_SPEED" +
                " and DirectionsCriteria.ANNOTATION_DISTANCE",
            failureMessage,
        )
    }

    @Test
    fun `should convert point to replay location`() {
        val point: Point = mockk {
            every { latitude() } returns 34.691564
            every { longitude() } returns 135.491059
        }

        val replayLocation = ReplayRouteMapper.mapToUpdateLocation(100.0, point)

        assertEquals(100.0, replayLocation.eventTimestamp, 0.01)
        assertEquals(34.691564, replayLocation.location.lat, 0.000001)
        assertEquals(135.491059, replayLocation.location.lon, 0.000001)
        assertEquals("ReplayRoute", replayLocation.location.provider)
        assertNull(replayLocation.location.altitude)
        assertNull(replayLocation.location.accuracyHorizontal)
        assertNull(replayLocation.location.bearing)
        assertNull(replayLocation.location.speed)
    }

    @Test
    fun `an artificial driver drives with almost constant speed along a motorway`() {
        val route = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("german_motorway_direction_route.json"),
        )

        val updateEvents = replayRouteMapper.mapDirectionsRouteGeometry(route)

        val speedUpdatesAmongARoute = updateEvents
            .filterIsInstance<ReplayEventUpdateLocation>().map {
                it.location.speed ?: 0.0
            }
            .removeAccelerationAndBrakingSpeedUpdates()
        val minSpeed = speedUpdatesAmongARoute.minOf { it }
        val maxSpeed = speedUpdatesAmongARoute.maxOf { it }
        assertTrue(
            "speed changes too much on the way: $speedUpdatesAmongARoute",
            maxSpeed - minSpeed < 1,
        )
    }

    @Test
    fun `should map geometry that is very long`() {
        val geometry = FileUtils.loadJsonFixture("replay_route_long_geometry.txt")

        val updateEvents = replayRouteMapper.mapGeometry(geometry)

        assertTrue(updateEvents.isNotEmpty())
        updateEvents.measureSpeedDistances().forEachIndexed { i, it ->
            assertEquals(it.locationSpeed, it.distanceSpeed, 1.0)
        }
    }

    @Test
    fun `distance speed should be near the estimated speed`() {
        val geometry = "kxia{Ao{daU??z@f@nAnAnAvBzEvBz@f@nAnA?rDzEg@bGg@bBg@nAwBzOgc@vBcGRcGg@" +
            "sDg@kCsI{JgEvVcGv[oFb[_I~a@_I~a@kC~MwBjMcB~HoA~H"
        val events = replayRouteMapper.mapGeometry(geometry)

        events.measureSpeedDistances().forEach {
            assertEquals(it.locationSpeed, it.distanceSpeed, 1.0)
        }
    }

    @Test
    fun `distance speed should be near the estimated speed for high frequency`() {
        val geometry = "kxia{Ao{daU??z@f@nAnAnAvBzEvBz@f@nAnA?rDzEg@bGg@bBg@nAwBzOgc@vBcGRcGg@" +
            "sDg@kCsI{JgEvVcGv[oFb[_I~a@_I~a@kC~MwBjMcB~HoA~H"
        val events = ReplayRouteMapper(
            ReplayRouteOptions.Builder().frequency(10.0).build(),
        ).mapGeometry(geometry)

        events.measureSpeedDistances().forEach {
            assertEquals(it.locationSpeed, it.distanceSpeed, 1.0)
        }
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.replay.route",
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
