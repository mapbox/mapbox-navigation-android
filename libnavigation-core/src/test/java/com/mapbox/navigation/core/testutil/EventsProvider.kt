package com.mapbox.navigation.core.testutil

import com.mapbox.navigation.core.telemetry.events.NavigationStepData
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import io.mockk.every
import io.mockk.mockk

internal object EventsProvider {

    fun provideDefaultTelemetryLocationsArray(
        telemetryLocations: Array<TelemetryLocation> = arrayOf(
            TelemetryLocation(
                latitude = 1.1,
                longitude = 2.2,
                speed = 3.3,
                bearing = 4.4,
                altitude = 5.5,
                timestamp = "timestamp_0",
                horizontalAccuracy = 6.6,
                verticalAccuracy = 7.7,
            ),
            TelemetryLocation(
                latitude = 1.2,
                longitude = 2.3,
                speed = 3.4,
                bearing = 4.5,
                altitude = 5.6,
                timestamp = "timestamp_1",
                horizontalAccuracy = 6.7,
                verticalAccuracy = 7.8,
            ),
        ),
    ): Array<TelemetryLocation> = telemetryLocations

    fun mockNavigationStepData(): NavigationStepData = mockk {
        every { durationRemaining } returns 1
        every { distance } returns 2
        every { distanceRemaining } returns 3
        every { duration } returns 4
        every { upcomingName } returns "upcomingName_0"
        every { upcomingModifier } returns "upcomingModifier_0"
        every { previousInstruction } returns "previousInstruction_0"
        every { previousName } returns "previousName_0"
        every { upcomingInstruction } returns "upcomingInstruction_0"
        every { previousType } returns "previousType_0"
        every { upcomingType } returns "upcomingType_0"
        every { previousModifier } returns "previousModifier_0"
    }
}
