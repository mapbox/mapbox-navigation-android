package com.mapbox.navigation.instrumentation_tests.utils.location

import android.location.Location
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

suspend fun BaseCoreNoCleanUpTest.stayOnPosition(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    frequencyHz: Int = 1,
    block: suspend () -> Unit
) {
    coroutineScope {
        val updateLocations = launch(start = CoroutineStart.UNDISPATCHED) {
            while (true) {
                mockLocationUpdatesRule.pushLocationUpdate {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.bearing = bearing
                }
                delay(1000L / frequencyHz)
            }
        }
        try {
            block()
        } finally {
            updateLocations.cancel()
        }
    }
}

suspend fun BaseCoreNoCleanUpTest.stayOnPosition(
    location: Location,
    frequencyHz: Int = 1,
    block: suspend () -> Unit
) {
    stayOnPosition(
        latitude = location.latitude,
        longitude = location.longitude,
        bearing = location.bearing,
        frequencyHz = frequencyHz,
        block = block,
    )
}

suspend fun BaseCoreNoCleanUpTest.stayOnPositionAndWaitForUpdate(
    mapboxNavigation: MapboxNavigation,
    latitude: Double,
    longitude: Double,
    bearing: Float,
    frequencyHz: Int = 1,
    block: suspend () -> Unit
) {
    stayOnPosition(latitude, longitude, bearing, frequencyHz) {
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(
                it.enhancedLocation.latitude - latitude
            ) < 0.0001 && abs(
                it.enhancedLocation.longitude - longitude
            ) < 0.0001
        }.first()
        block()
    }
}
