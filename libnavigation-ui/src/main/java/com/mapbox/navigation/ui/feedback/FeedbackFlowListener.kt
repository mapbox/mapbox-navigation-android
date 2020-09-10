package com.mapbox.navigation.ui.feedback

import com.mapbox.navigation.core.telemetry.events.CachedNavigationFeedbackEvent

/**
 * Interface notified on interaction with the possible feedback opportunities
 * that the Navigation UI SDK provides.
 */
interface FeedbackFlowListener {
    /**
     * The callback gets triggered when [FeedbackDetailsFragment] flow finishes.
     *
     * @param cachedNavigationFeedbackEvents updated feedback events
     */
    fun onDetailedFeedbackFlowFinished(
        cachedNavigationFeedbackEvents: List<CachedNavigationFeedbackEvent>
    )

    /**
     * The callback gets triggered when [FeedbackArrivalFragment] flow finishes.
     *
     * @param arrivalFeedbackItem the overall experience of a navigation
     */
    fun onArrivalExperienceFeedbackFinished(arrivalFeedbackItem: FeedbackItem)
}
