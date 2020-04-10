package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent

internal interface MapboxNavigationTelemetryInterface {
    fun postUserFeedback(
        @FeedbackEvent.FeedbackType feedbackType: String,
        description: String,
        @FeedbackEvent.FeedbackSource feedbackSource: String,
        screenshot: String?
    )

    fun unregisterListeners(mapboxNavigation: MapboxNavigation)
}
