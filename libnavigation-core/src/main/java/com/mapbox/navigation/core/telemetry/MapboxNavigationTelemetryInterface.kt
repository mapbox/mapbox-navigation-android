package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.TelemetryUserFeedback
import kotlinx.coroutines.Job

interface MapboxNavigationTelemetryInterface {
    fun postUserFeedback(
        @TelemetryUserFeedback.FeedbackType feedbackType: String,
        description: String,
        @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
        screenshot: String?
    )

    fun unregisterListeners(mapboxNavigation: MapboxNavigation): Job
}
