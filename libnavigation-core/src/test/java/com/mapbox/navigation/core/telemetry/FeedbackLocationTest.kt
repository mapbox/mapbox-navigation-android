package com.mapbox.navigation.core.telemetry

import com.google.gson.Gson
import com.mapbox.common.location.Location
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FeedbackLocationTest {

    private val location = mockk<Location>()
    private val gson = Gson()

    @Before
    fun setUp() {
        every { location.latitude } returns LATITUDE
        every { location.longitude } returns LONGITUDE
        every { location.speed } returns SPEED
        every { location.bearing } returns BEARING
        every { location.altitude } returns ALTITUDE
        every { location.timestamp } returns TIMESTAMP
        every { location.horizontalAccuracy } returns HORIZONTAL_ACCURACY
        every { location.verticalAccuracy } returns VERTICAL_ACCURACY
    }

    @Test
    fun checkLocationSerialization() {
        val feedbackLocation = TelemetryLocation(
            location.latitude,
            location.longitude,
            location.speed,
            location.bearing,
            location.altitude,
            location.timestamp.toString(),
            location.horizontalAccuracy!!,
            location.verticalAccuracy!!,
        )

        val feedbackLocationJson = gson.toJson(feedbackLocation)
        val deserializedFeedbackLocation =
            gson.fromJson(feedbackLocationJson, TelemetryLocation::class.java)

        deserializedFeedbackLocation.run {
            assertEquals(location.latitude, latitude, 0.0)
            assertEquals(location.longitude, longitude, 0.0)
            assertEquals(location.speed, speed)
            assertEquals(location.bearing, bearing)
            assertEquals(location.altitude!!, altitude!!, 0.0)
            assertEquals(location.timestamp.toString(), timestamp)
            assertEquals(location.horizontalAccuracy, horizontalAccuracy)
            assertEquals(location.verticalAccuracy, verticalAccuracy)
        }
    }

    companion object {
        private const val LATITUDE = 1.1
        private const val LONGITUDE = 2.2
        private const val SPEED = 30.0
        private const val BEARING = 20.0
        private const val ALTITUDE = 10.0
        private const val TIMESTAMP = 999999L
        private const val HORIZONTAL_ACCURACY = 1.0
        private const val VERTICAL_ACCURACY = 2.0
    }
}
