package com.mapbox.navigation.core.navigator

import android.location.Location
import android.os.Bundle
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class LocationExTest {

    @Test
    fun toFixLocation() {
        val location = Location(PROVIDER).apply {
            latitude = LATITUDE
            longitude = LONGITUDE
            time = TIME
            elapsedRealtimeNanos = ELAPSED_REAL_TIME
            speed = SPEED
            bearing = BEARING
            altitude = ALTITUDE
            accuracy = ACCURACY
            bearingAccuracyDegrees = BEARING_ACCURACY
            speedAccuracyMetersPerSecond = SPEED_ACCURACY
            verticalAccuracyMeters = VERTICAL_ACCURACY
            extras = Bundle()
        }
        Location::class.java.getDeclaredMethod(
            "setIsFromMockProvider",
            Boolean::class.java
        ).invoke(location, IS_MOCK)

        location.toFixLocation().run {
            assertEquals(LATITUDE, coordinate.latitude(), .0)
            assertEquals(LONGITUDE, coordinate.longitude(), .0)
            assertEquals(ELAPSED_REAL_TIME, monotonicTimestampNanoseconds)
            assertEquals(Date(TIME), time)
            assertEquals(SPEED, speed!!, .0f)
            assertEquals(BEARING, bearing!!, .0f)
            assertEquals(ALTITUDE, altitude!!.toDouble(), .0)
            assertEquals(ACCURACY, accuracyHorizontal!!, .0f)
            assertEquals(PROVIDER, provider)
            assertEquals(BEARING_ACCURACY, bearingAccuracy!!, .0f)
            assertEquals(SPEED_ACCURACY, speedAccuracy!!, .0f)
            assertEquals(VERTICAL_ACCURACY, verticalAccuracy!!, .0f)
            assertEquals(EMPTY_EXTRAS, extras.toMap())
            assertEquals(IS_MOCK, isMock)
        }
    }

    @Test
    fun toLocation() {
        val fixLocation = FixLocation(
            Point.fromLngLat(LONGITUDE, LATITUDE),
            ELAPSED_REAL_TIME,
            DATE,
            SPEED,
            BEARING,
            ALTITUDE.toFloat(),
            ACCURACY,
            PROVIDER,
            BEARING_ACCURACY,
            SPEED_ACCURACY,
            VERTICAL_ACCURACY,
            EMPTY_EXTRAS,
            IS_MOCK,
        )

        fixLocation.toLocation().run {
            assertEquals(LATITUDE, latitude, .0)
            assertEquals(LONGITUDE, longitude, .0)
            assertEquals(ELAPSED_REAL_TIME, elapsedRealtimeNanos)
            assertEquals(DATE, Date(time))
            assertEquals(SPEED, speed, .0f)
            assertEquals(BEARING, bearing, .0f)
            assertEquals(ALTITUDE, altitude, .0)
            assertEquals(ACCURACY, accuracy, .0f)
            assertEquals(PROVIDER, provider)
            assertEquals(BEARING_ACCURACY, bearingAccuracyDegrees, .0f)
            assertEquals(SPEED_ACCURACY, speedAccuracyMetersPerSecond, .0f)
            assertEquals(VERTICAL_ACCURACY, verticalAccuracyMeters, .0f)
            assertEquals(EMPTY_BUNDLE.toString(), EMPTY_EXTRAS.toBundle().toString())
            assertEquals(IS_MOCK, isFromMockProvider)
        }
    }

    @Test
    fun checkLocationWithZeroParams() {
        val fixLocation = FixLocation(
            Point.fromLngLat(LONGITUDE, LATITUDE),
            ELAPSED_REAL_TIME,
            DATE,
            NULL_VALUE,
            NULL_VALUE,
            NULL_VALUE,
            NULL_VALUE,
            PROVIDER,
            NULL_VALUE,
            NULL_VALUE,
            NULL_VALUE,
            EMPTY_EXTRAS,
            IS_MOCK,
        )

        fixLocation.toLocation().run {
            assertFalse(hasSpeed())
            assertFalse(hasBearing())
            assertFalse(hasAltitude())
            assertFalse(hasAccuracy())
            assertFalse(hasBearingAccuracy())
            assertFalse(hasSpeedAccuracy())
            assertFalse(hasVerticalAccuracy())

            assertEquals(ZERO_VALUE, speed, .0f)
            assertEquals(ZERO_VALUE, bearing, .0f)
            assertEquals(ZERO_VALUE, altitude.toFloat())
            assertEquals(ZERO_VALUE, accuracy, .0f)
            assertEquals(ZERO_VALUE, bearingAccuracyDegrees, .0f)
            assertEquals(ZERO_VALUE, speedAccuracyMetersPerSecond, .0f)
            assertEquals(ZERO_VALUE, verticalAccuracyMeters, .0f)
            assertEquals(EMPTY_BUNDLE.toString(), extras.toString())
            assertEquals(IS_MOCK, isFromMockProvider)
        }
    }

    companion object {
        private val NULL_VALUE = null
        private val DATE = Date()
        private const val ZERO_VALUE = .0f
        private const val PROVIDER = "Test"
        private const val LATITUDE = 11.0
        private const val LONGITUDE = 22.0
        private const val TIME = 22222L
        private const val ELAPSED_REAL_TIME = 11111L
        private const val SPEED = 10f
        private const val BEARING = 20f
        private const val ALTITUDE = 30.0
        private const val ACCURACY = 40f
        private const val BEARING_ACCURACY = 50f
        private const val SPEED_ACCURACY = 60f
        private const val VERTICAL_ACCURACY = 70f
        private val EMPTY_EXTRAS = FixLocationExtras()
        private val EMPTY_BUNDLE = Bundle()
        private const val IS_MOCK = true
    }
}
