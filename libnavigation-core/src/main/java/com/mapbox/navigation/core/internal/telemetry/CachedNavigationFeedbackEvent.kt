package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.navigation.core.telemetry.events.FeedbackEvent

/**
 * A cache version of an internal MapboxTelemetry feedback event.
 * It holds the required data for Feedback UI to display and to be updated with user input.
 *
 * @param feedbackId the key of the feedback
 * @param feedbackType one of [FeedbackEvent.Type]
 * @param screenshot encoded screenshot (optional)
 * @param description the user's additional comment about the feedback (optional)
 * @param feedbackSubType array of [FeedbackEvent.SubType] (optional)
 */
data class CachedNavigationFeedbackEvent @JvmOverloads internal constructor(
    val feedbackId: String,
    @FeedbackEvent.Type
    val feedbackType: String,
    val screenshot: String,
    var description: String? = null,
    val feedbackSubType: MutableSet<String> = HashSet()
)
