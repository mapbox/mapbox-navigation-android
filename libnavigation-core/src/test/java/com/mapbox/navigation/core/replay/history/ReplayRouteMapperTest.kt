package com.mapbox.navigation.core.replay.history

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReplayRouteMapperTest {

    private val replayRouteMapper = ReplayRouteMapper()

    @Test
    fun `should map route with waypoints`() {
        val geometry = """glp_gA~ahmhFV`H?j@d@nTz@r[|@j`@aCxBgYdYgCl@yM\gkBnIkVjA"""

        val replayEvents = replayRouteMapper.mapToUpdateLocations(0.0, geometry)

        assertTrue(replayEvents.isNotEmpty())
    }

    @Test
    fun `should map android location`() {
        val location: Location = mockk {
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

        val replayEvents = replayRouteMapper.mapToUpdateLocation(0.1, location)

        assertEquals(replayEvents.size, 1)
        val locationUpdate = replayEvents[0] as ReplayEventUpdateLocation
        assertEquals(locationUpdate.eventTimestamp, 0.1)
        assertEquals(locationUpdate.location.lat, -122.392624)
        assertEquals(locationUpdate.location.lon, 37.764107)
        assertEquals(locationUpdate.location.accuracyHorizontal, 11.0)
        assertEquals(locationUpdate.location.bearing, 12.0)
        assertEquals(locationUpdate.location.speed, 2.0)
        assertEquals(locationUpdate.location.altitude, 25.0)
    }

    @Test
    fun `should map android location with optional`() {
        val location: Location = mockk {
            every { latitude } returns -122.392624
            every { longitude } returns 37.764107
            every { hasAccuracy() } returns false
            every { hasBearing() } returns false
            every { hasSpeed() } returns false
            every { hasAltitude() } returns false
        }

        val replayEvents = replayRouteMapper.mapToUpdateLocation(0.1, location)

        assertEquals(replayEvents.size, 1)
        val locationUpdate = replayEvents[0] as ReplayEventUpdateLocation
        assertEquals(locationUpdate.eventTimestamp, 0.1)
        assertEquals(locationUpdate.location.lat, -122.392624)
        assertEquals(locationUpdate.location.lon, 37.764107)
        assertNull(locationUpdate.location.accuracyHorizontal)
        assertNull(locationUpdate.location.bearing)
        assertNull(locationUpdate.location.speed)
        assertNull(locationUpdate.location.altitude)
    }
}
