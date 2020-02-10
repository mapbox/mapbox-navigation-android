package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.navigation.utils.extensions.toFixLocation
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationExTest {

    @Test
    fun toFixLocation() {
        val location = Location("test").apply {
            latitude = 11.0
            longitude = 22.0
            time = 123456789
            speed = 10f
            bearing = 20f
            altitude = 30.0
            accuracy = 40f
        }

        val date = Date()
        val fix = location.toFixLocation(date)
        assertEquals(11.0, fix.coordinate.latitude(), .00001)
        assertEquals(22.0, fix.coordinate.longitude(), .00001)
        assertEquals(date, fix.time)
        assertEquals(10f, fix.speed!!, .00001f)
        assertEquals(20f, fix.bearing!!, .00001f)
        assertEquals(30.0, fix.altitude!!.toDouble(), .00001)
        assertEquals(40f, fix.accuracyHorizontal!!, .00001f)
    }
}
