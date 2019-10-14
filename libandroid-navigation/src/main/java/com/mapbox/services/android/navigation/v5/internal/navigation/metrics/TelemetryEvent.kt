package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

internal interface TelemetryEvent {

    val eventId: String

    val sessionState: SessionState
}
