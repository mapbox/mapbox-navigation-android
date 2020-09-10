package com.mapbox.navigation.ui

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.CachedNavigationFeedbackEvent
import com.mapbox.navigation.ui.feedback.FeedbackItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NavigationFeedbackFlowListenerTest {
    @Test
    fun onDetailedFeedbackFlowFinished() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
        every { navigationViewModel.retrieveNavigation() } returns mapboxNavigation
        val listener = NavigationFeedbackFlowListener(navigationViewModel)
        val cachedFeedbackEventList = mockk<List<CachedNavigationFeedbackEvent>>()

        listener.onDetailedFeedbackFlowFinished(cachedFeedbackEventList)

        verify { navigationViewModel.onDetailedFeedbackFlowFinished() }
        verify { mapboxNavigation.postCachedUserFeedback(cachedFeedbackEventList) }
    }

    @Test
    fun onArrivalExperienceFeedbackFinished() {
        val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
        val listener = NavigationFeedbackFlowListener(navigationViewModel)
        val cachedFeedbackEventList = mockk<FeedbackItem>()

        listener.onArrivalExperienceFeedbackFinished(cachedFeedbackEventList)

        verify { navigationViewModel.updateFeedback(cachedFeedbackEventList) }
    }
}
