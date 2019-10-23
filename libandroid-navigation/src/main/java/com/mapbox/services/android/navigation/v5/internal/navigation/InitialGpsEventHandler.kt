package com.mapbox.services.android.navigation.v5.internal.navigation

import com.google.gson.Gson
import com.mapbox.navigation.metrics.NavigationMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.MapboxMetricsReporter

internal class InitialGpsEventHandler {

    fun send(
        elapsedTime: Double,
        sessionId: String,
        metadata: NavigationPerformanceMetadata,
        gson: Gson
    ) {
        val event = InitialGpsEvent(elapsedTime, sessionId, metadata)
        MapboxMetricsReporter.sendInitialGpsEvent(
            NavigationMetrics.INITIAL_GPS,
            gson.toJson(event)
        )
    }
}
