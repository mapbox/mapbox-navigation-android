package com.mapbox.navigation.testing.factories

import com.mapbox.common.location.Location
import io.mockk.every
import io.mockk.mockk

fun createLocation(
    longitude: Double = 0.0,
    latitude: Double = 0.0,
    bearing: Double = 0.0,
) = mockk<Location>(
    relaxed = true
) {
    val location = this
    every { location.longitude } returns longitude
    every { location.latitude } returns latitude
    every { location.bearing } returns bearing
    every { extra } returns null
}
