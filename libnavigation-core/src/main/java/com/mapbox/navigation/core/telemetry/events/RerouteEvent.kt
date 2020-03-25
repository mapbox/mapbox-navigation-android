package com.mapbox.navigation.core.telemetry.events

import com.mapbox.android.telemetry.TelemetryUtils.obtainUniversalUniqueIdentifier

class RerouteEvent(
    override var sessionState: SessionState
) : TelemetryEvent {
    override val eventId: String = obtainUniversalUniqueIdentifier()
    var newRouteGeometry: String = ""
    var newDurationRemaining: Int = 0
    var newDistanceRemaining: Int = 0
}
