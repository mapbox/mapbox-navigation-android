package com.mapbox.navigation.core.telemetry.events

/**
 * A cache version of an internal MapboxTelemetry feedback event.
 * It holds the required data for Feedback UI to display and to be updated with user input.
 *
 * @param feedbackId the key of the feedback
 * @param feedbackType one of [FeedbackEvent.Type]
 * @param description the user's additional comment about the feedback (optional)
 * @param screenshot encoded screenshot (optional)
 * @param feedbackSubType array of [FeedbackEvent.Description] (optional)
 */
class CachedNavigationFeedbackEvent internal constructor(
    val feedbackId: String,
    @FeedbackEvent.Type
    val feedbackType: String,
    var description: String? = null,
    val screenshot: String,
    val feedbackSubType: MutableSet<String> = HashSet()
)
