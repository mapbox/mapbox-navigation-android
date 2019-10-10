package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import com.mapbox.android.telemetry.TelemetryUtils

class RerouteEvent(
    override var sessionState: SessionState
) : TelemetryEvent {
    override val eventId: String = TelemetryUtils.obtainUniversalUniqueIdentifier()
    var newRouteGeometry: String = ""
    var newDurationRemaining: Int = 0
    var newDistanceRemaining: Int = 0
}
