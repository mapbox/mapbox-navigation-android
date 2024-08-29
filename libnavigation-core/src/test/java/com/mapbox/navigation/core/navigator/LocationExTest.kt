package com.mapbox.navigation.core.navigator

import com.mapbox.bindgen.Value
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class LocationExTest {

    @Test
    fun toFixLocation() {
        val location = Location.Builder()
            .source(PROVIDER)
            .latitude(LATITUDE)
            .longitude(LONGITUDE)
            .timestamp(TIME)
            .monotonicTimestamp(ELAPSED_REAL_TIME)
            .speed(SPEED.toDouble())
            .bearing(BEARING.toDouble())
            .altitude(ALTITUDE)
            .horizontalAccuracy(ACCURACY.toDouble())
            .bearingAccuracy(BEARING_ACCURACY.toDouble())
            .speedAccuracy(SPEED_ACCURACY.toDouble())
            .verticalAccuracy(VERTICAL_ACCURACY.toDouble())
            .extra(EXTRA)
            .build()

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
            assertEquals(EXTRAS, extras.toMap())
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
            EXTRAS,
            IS_MOCK,
        )

        fixLocation.toLocation().run {
            assertEquals(LATITUDE, latitude, .0)
            assertEquals(LONGITUDE, longitude, .0)
            assertEquals(ELAPSED_REAL_TIME, monotonicTimestamp)
            assertEquals(DATE, Date(timestamp))
            assertEquals(SPEED.toDouble(), speed!!, .0)
            assertEquals(BEARING.toDouble(), bearing!!, .0)
            assertEquals(ALTITUDE, altitude!!, .0)
            assertEquals(ACCURACY.toDouble(), horizontalAccuracy!!, .0)
            assertEquals(PROVIDER, source)
            assertEquals(BEARING_ACCURACY.toDouble(), bearingAccuracy!!, .0)
            assertEquals(SPEED_ACCURACY.toDouble(), speedAccuracy!!, .0)
            assertEquals(VERTICAL_ACCURACY.toDouble(), verticalAccuracy!!, .0)
            assertEquals(EXTRA, extra)
        }
    }

    @Test
    fun checkLocationWithNullParams() {
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
            assertNull(speed)
            assertNull(bearing)
            assertNull(altitude)
            assertNull(horizontalAccuracy)
            assertNull(bearingAccuracy)
            assertNull(speedAccuracy)
            assertNull(verticalAccuracy)

            assertEquals(
                hashMapOf("is_mock" to Value.valueOf(IS_MOCK)),
                extra!!.contents as HashMap<String, Value>,
            )
        }
    }

    companion object {
        private val NULL_VALUE = null
        private val DATE = Date()
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
        private val EXTRAS = FixLocationExtras().also {
            it["satellites"] = Value(42)
            it["string"] = Value("str42")
        }
        private const val IS_MOCK = true
        private val EXTRA = Value.valueOf(
            hashMapOf(
                "satellites" to Value.valueOf(42),
                "string" to Value.valueOf("str42"),
                "is_mock" to Value.valueOf(true),
            ),
        )
        private val EMPTY_EXTRAS = FixLocationExtras()
    }
}
