package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.navigator.toFixLocation
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplayEventLocationMapperTest {
    @Test
    fun `map fields as they are`() {
        val eventsLocation = createTestEventLocation()

        val commonLocation = eventsLocation.mapToLocation(eventsLocation.time!!)

        assertEquals(eventsLocation.lon, commonLocation.longitude)
        assertEquals(eventsLocation.lat, commonLocation.latitude)
        assertEquals(eventsLocation.provider, commonLocation.source)
        assertEquals(eventsLocation.altitude, commonLocation.altitude)
        assertEquals(
            eventsLocation.accuracyHorizontal!!,
            commonLocation.horizontalAccuracy!!,
            0.0001,
        )
        assertEquals(
            eventsLocation.bearing!!,
            commonLocation.bearing!!,
            0.0001,
        )
        assertEquals(
            eventsLocation.speed!!,
            commonLocation.speed!!,
            0.0001,
        )
    }

    @Test
    fun `add timestamp to current time`() {
        val eventsLocation = createTestEventLocation(
            locationOffsetInSeconds = 2.0,
        )

        val androidLocation = eventsLocation.mapToLocation(
            eventsLocation.time!!,
            currentTimeMilliseconds = 1_000,
            elapsedTimeNano = 1_000_000_000,
        )

        assertEquals(3000, androidLocation.timestamp)
        assertEquals(3000000000, androidLocation.monotonicTimestamp)
    }

    @Test
    fun `replay location is always mocked`() {
        val eventsLocation = createTestEventLocation()

        val fixLocation = eventsLocation
            .mapToLocation(eventsLocation.time!!)
            .toFixLocation()

        assertTrue(fixLocation.isMock)
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
    speed = 0.5585000514984131,
)
