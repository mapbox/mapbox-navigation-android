package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.NavigationMetrics

internal class InitialGpsEventHandler(
    private val metricsReporter: MetricsReporter
) {

    fun send(
        elapsedTime: Double,
        sessionId: String,
        metadata: NavigationPerformanceMetadata
    ) {
        val event = InitialGpsEvent(elapsedTime, sessionId, metadata)
        metricsReporter.addEvent(NavigationMetrics.INITIAL_GPS, event)
    }
}
