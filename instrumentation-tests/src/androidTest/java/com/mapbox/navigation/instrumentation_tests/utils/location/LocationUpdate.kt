package com.mapbox.navigation.instrumentation_tests.utils.location

import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun BaseCoreNoCleanUpTest.stayOnPosition(
    latitude: Double,
    longitude: Double,
    frequencyHz: Int = 10,
    block: suspend () -> Unit
) {
    coroutineScope {
        val updateLocations = launch {
            while (true) {
                mockLocationUpdatesRule.pushLocationUpdate {
                    this.latitude = latitude
                    this.longitude = longitude
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