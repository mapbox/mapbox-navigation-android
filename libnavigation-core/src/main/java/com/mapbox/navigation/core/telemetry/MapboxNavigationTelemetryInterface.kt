package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent

internal interface MapboxNavigationTelemetryInterface {
    fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?
    )

    fun unregisterListeners(mapboxNavigation: MapboxNavigation)
}
