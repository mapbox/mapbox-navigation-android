package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

interface TelemetryEvent {

    val eventId: String

    val sessionState: SessionState
}
