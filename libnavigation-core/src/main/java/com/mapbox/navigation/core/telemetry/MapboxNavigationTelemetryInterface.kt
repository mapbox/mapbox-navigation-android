package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.core.telemetry.telemetryevents.TelemetryUserFeedback

interface MapboxNavigationTelemetryInterface {
    fun postUserFeedbackEvent(
        @TelemetryUserFeedback.FeedbackType feedbackType: String,
        description: String,
        @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
        scrShot: String?
    )
}
