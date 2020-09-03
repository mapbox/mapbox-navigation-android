package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TelemetryLocationAndProgressDispatcherTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val locationAndProgressDispatcher = TelemetryLocationAndProgressDispatcher(
        ThreadController.getMainScopeAndRootJob().scope
    )

    @Test
    fun ignoreEnhancedLocationUpdates() {
        val enhancedLocation: Location = mockk()
        locationAndProgressDispatcher.onEnhancedLocationChanged(enhancedLocation, mockk())

        assertFalse(
            "this job should not be completed",
            locationAndProgressDispatcher.getFirstLocationAsync().isCompleted
        )
        assertNull(locationAndProgressDispatcher.getLastLocation())
    }

    @Test
    fun useRawLocationUpdates() {
        val rawLocation: Location = mockk()
        locationAndProgressDispatcher.onRawLocationChanged(rawLocation)

        assertEquals(
            rawLocation,
            locationAndProgressDispatcher.getFirstLocationAsync().getCompleted()
        )
        assertEquals(rawLocation, locationAndProgressDispatcher.getLastLocation())
    }
}
