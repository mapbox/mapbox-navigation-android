package com.mapbox.navigation.ui

import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent
import com.mapbox.navigation.core.internal.telemetry.MapboxNavigationFeedbackCache
import com.mapbox.navigation.ui.feedback.FeedbackItem
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NavigationFeedbackFlowListenerTest {

    @Before
    fun setup() {
        mockkObject(MapboxNavigationFeedbackCache)
        every {
            MapboxNavigationFeedbackCache.postCachedUserFeedback(any())
        } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxNavigationFeedbackCache)
    }

    @Test
    fun onDetailedFeedbackFlowFinished() {
        val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
        val listener = NavigationFeedbackFlowListener(navigationViewModel)
        val cachedFeedbackEventList = mockk<List<CachedNavigationFeedbackEvent>>()

        listener.onDetailedFeedbackFlowFinished(cachedFeedbackEventList)

        verify { navigationViewModel.onDetailedFeedbackFlowFinished() }
        verify { MapboxNavigationFeedbackCache.postCachedUserFeedback(cachedFeedbackEventList) }
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
