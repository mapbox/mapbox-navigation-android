package com.mapbox.navigation.core.replay.history

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplayEventLocationMapperTest {
    @Test
    fun `map fields as they are`() {
        val eventsLocation = createTestEventLocation()

        val androidLocation = eventsLocation.mapToLocation(eventsLocation.time!!)

        assertEquals(eventsLocation.lon, androidLocation.longitude)
        assertEquals(eventsLocation.lat, androidLocation.latitude)
        assertEquals(eventsLocation.provider, androidLocation.provider)
        assertEquals(eventsLocation.altitude, androidLocation.altitude)
        assertEquals(
            eventsLocation.accuracyHorizontal!!.toFloat(),
            androidLocation.accuracy,
            0.0001f
        )
        assertEquals(
            eventsLocation.bearing!!.toFloat(),
            androidLocation.bearing,
            0.0001f
        )
        assertEquals(
            eventsLocation.speed!!.toFloat(),
            androidLocation.speed,
            0.0001f
        )
    }

    @Test
    fun `add timestamp to current time`() {
        val eventsLocation = createTestEventLocation(
            locationOffsetInSeconds = 2.0
        )

        val androidLocation = eventsLocation.mapToLocation(
            eventsLocation.time!!,
            currentTimeMilliseconds = 1_000,
            elapsedTimeNano = 1_000_000_000
        )

        assertEquals(3000, androidLocation.time)
        assertEquals(3000000000, androidLocation.elapsedRealtimeNanos)
    }
}

private fun createTestEventLocation(
    locationOffsetInSeconds: Double = 1580777612.892,
): ReplayEventLocation = ReplayEventLocation(
    lat = 49.2492411,
    lon = 8.8512315,
    provider = "fused",
    time = locationOffsetInSeconds,
    altitude = 212.4732666015625,
    accuracyHorizontal = 4.288000106811523,
    bearing = 243.31265258789063,
    speed = 0.5585000514984131
)
