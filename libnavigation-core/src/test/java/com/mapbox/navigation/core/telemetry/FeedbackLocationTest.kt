package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.google.gson.Gson
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
        every { location.time } returns TIMESTAMP
        every { location.accuracy } returns HORIZONTAL_ACCURACY
        every { location.verticalAccuracyMeters } returns VERTICAL_ACCURACY
    }

    @Test
    fun checkLocationSerialization() {
        val feedbackLocation = TelemetryLocation(
            location.latitude,
            location.longitude,
            location.speed,
            location.bearing,
            location.altitude,
            location.time.toString(),
            location.accuracy,
            location.verticalAccuracyMeters
        )

        val feedbackLocationJson = gson.toJson(feedbackLocation)
        val deserializedFeedbackLocation =
            gson.fromJson(feedbackLocationJson, TelemetryLocation::class.java)

        deserializedFeedbackLocation.run {
            assertEquals(location.latitude, latitude, 0.0)
            assertEquals(location.longitude, longitude, 0.0)
            assertEquals(location.speed, speed)
            assertEquals(location.bearing, bearing)
            assertEquals(location.altitude, altitude, 0.0)
            assertEquals(location.time.toString(), timestamp)
            assertEquals(location.accuracy, horizontalAccuracy)
            assertEquals(location.verticalAccuracyMeters, verticalAccuracy)
        }
    }

    companion object {
        private const val LATITUDE = 1.1
        private const val LONGITUDE = 2.2
        private const val SPEED = 30f
        private const val BEARING = 200f
        private const val ALTITUDE = 10.0
        private const val TIMESTAMP = 999999L
        private const val HORIZONTAL_ACCURACY = 1f
        private const val VERTICAL_ACCURACY = 2f
    }
}
