package com.mapbox.navigation.core.infra.factories

import android.location.Location
import io.mockk.every
import io.mockk.mockk

fun createLocation(
    longitude: Double = 0.0,
    latitude: Double = 0.0,
    bearing: Double = 0.0,
) = mockk<Location>(relaxed = true).apply {
    every { this@apply.latitude } returns latitude
    every { this@apply.longitude } returns longitude
    every { this@apply.bearing } returns bearing.toFloat()
}
