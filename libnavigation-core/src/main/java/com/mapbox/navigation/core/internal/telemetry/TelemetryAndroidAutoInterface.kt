package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.navigation.core.internal.telemetry.event.AndroidAutoEvent

interface TelemetryAndroidAutoInterface {
    fun postAndroidAutoEvent(event: AndroidAutoEvent)
}
