package com.mapbox.navigation.testing.factories

import android.location.Location
import io.mockk.every
import io.mockk.mockk

fun createLocation(
    longitude: Double = 0.0,
    latitude: Double = 0.0,
    bearing: Float = 0.0f,
) = mockk<Location>(
    relaxed = true
) {
    val location = this
    every { location.longitude } returns longitude
    every { location.latitude } returns latitude
    every { location.bearing } returns bearing
    every { extras } returns mockk(relaxed = true) {
        every { keySet() } returns emptySet<String>()
    }
}
