package com.mapbox.navigation.core.internal.utils

import android.location.Location
import android.os.Build
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation

internal fun List<Location>.toTelemetryLocations(): Array<TelemetryLocation> {
    val feedbackLocations = mutableListOf<TelemetryLocation>()
    forEach {
        feedbackLocations.add(it.toTelemetryLocation())
    }
    return feedbackLocations.toTypedArray()
}

internal fun Location.toTelemetryLocation(): TelemetryLocation {
    return TelemetryLocation(
        latitude,
        longitude,
        speed,
        bearing,
        altitude,
        time.toString(),
        accuracy,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verticalAccuracyMeters
        } else {
            0f
        }
    )
}
