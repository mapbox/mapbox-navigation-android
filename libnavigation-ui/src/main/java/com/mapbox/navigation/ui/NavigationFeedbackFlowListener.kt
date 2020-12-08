package com.mapbox.navigation.ui

import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent
import com.mapbox.navigation.core.internal.telemetry.MapboxNavigationFeedbackCache
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.internal.feedback.FeedbackFlowListener

/**
 * Internal callback for NavigationView to handle Feedback flow changing event.
 */
internal class NavigationFeedbackFlowListener(
    private val navigationViewModel: NavigationViewModel
) : FeedbackFlowListener {

    override fun onDetailedFeedbackFlowFinished(
        cachedNavigationFeedbackEvents: List<CachedNavigationFeedbackEvent>
    ) {
        navigationViewModel.onDetailedFeedbackFlowFinished()
        MapboxNavigationFeedbackCache.postCachedUserFeedback(cachedNavigationFeedbackEvents)
    }

    override fun onArrivalExperienceFeedbackFinished(arrivalFeedbackItem: FeedbackItem) {
        navigationViewModel.updateFeedback(arrivalFeedbackItem)
    }

    override fun onFeedbackFlowFinished() {
        // do nothing
    }
}
