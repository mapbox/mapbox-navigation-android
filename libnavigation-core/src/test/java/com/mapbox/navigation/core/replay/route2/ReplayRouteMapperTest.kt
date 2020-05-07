package com.mapbox.navigation.core.replay.route2

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteMapperTest {

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
    fun `should map android location`() {
        val location: Location = mockk {
            every { provider } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { hasAccuracy() } returns true
            every { accuracy } returns 11.0f
            every { hasBearing() } returns true
            every { bearing } returns 12.0f
            every { hasSpeed() } returns true
            every { speed } returns 2.0f
            every { hasAltitude() } returns true
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
            every { provider } returns "test provider"
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { hasAccuracy() } returns false
            every { hasBearing() } returns false
            every { hasSpeed() } returns false
            every { hasAltitude() } returns false
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
}
