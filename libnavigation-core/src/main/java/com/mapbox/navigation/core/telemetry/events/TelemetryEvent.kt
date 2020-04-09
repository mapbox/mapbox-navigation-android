package com.mapbox.navigation.core.telemetry.events

internal interface TelemetryEvent {

    val eventId: String

    val sessionState: SessionState
}
