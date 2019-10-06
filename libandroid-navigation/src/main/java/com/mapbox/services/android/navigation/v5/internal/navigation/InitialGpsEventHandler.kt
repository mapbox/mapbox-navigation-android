package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationMetricsWrapper.sendInitialGpsEvent

internal class InitialGpsEventHandler {

    fun send(
        elapsedTime: Double,
        sessionId: String,
        metadata: NavigationPerformanceMetadata?
    ) {
        sendInitialGpsEvent(elapsedTime, sessionId, metadata)
    }
}
