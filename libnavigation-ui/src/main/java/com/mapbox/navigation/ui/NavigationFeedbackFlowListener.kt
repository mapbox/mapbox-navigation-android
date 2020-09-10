package com.mapbox.navigation.ui

import com.mapbox.navigation.core.telemetry.events.CachedNavigationFeedbackEvent
import com.mapbox.navigation.ui.feedback.FeedbackFlowListener
import com.mapbox.navigation.ui.feedback.FeedbackItem

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
        navigationViewModel.retrieveNavigation()?.run {
            postCachedUserFeedback(cachedNavigationFeedbackEvents)
        }
    }

    override fun onArrivalExperienceFeedbackFinished(arrivalFeedbackItem: FeedbackItem) {
        navigationViewModel.updateFeedback(arrivalFeedbackItem)
    }
}
