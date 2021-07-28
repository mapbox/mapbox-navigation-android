package com.mapbox.navigation.core.telemetry.events

import java.util.Date

data class FeedbackMetadata internal constructor(
    internal val sessionIdentifier: String,
    internal val driverModeIdentifier: String,
    @FeedbackEvent.DriverMode internal val driverMode: String,
    internal val driverModeStartTime: Date = Date()
)
