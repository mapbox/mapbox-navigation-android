package com.mapbox.navigation.ui

import com.mapbox.navigation.ui.feedback.FeedbackFlowListener
import com.mapbox.navigation.ui.feedback.FeedbackItem

/**
 * Internal callback for NavigationView to handle Guidance view visibility changing event.
 */
internal class NavigationFeedbackFlowListener(
    private val navigationViewModel: NavigationViewModel
) : FeedbackFlowListener {

    override fun onDetailedFeedbackFlowFinished() {
        navigationViewModel.cachedFeedbackItems?.let { itemList ->
            if (itemList.isNotEmpty()) {
                navigationViewModel.sendCachedFeedback()
            }
        }
    }

    override fun onArrivalExperienceFeedbackFinished(arrivalFeedbackItem: FeedbackItem) {
        navigationViewModel.cachedFeedbackItems?.let { itemList ->
            if (itemList.isNotEmpty()) {
                navigationViewModel.sendCachedFeedback()
            }
            navigationViewModel.sendFeedback(arrivalFeedbackItem)
        }
    }
}
