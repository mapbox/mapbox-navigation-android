package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.retrieveCompositeHistoryRecorder
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteLineHistoryRecordingEnabledObserverTest {

    private val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    private val compositeRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private val manualRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private val recorderObserver = mockk<(MapboxHistoryRecorder?) -> Unit>(relaxed = true)
    private lateinit var observer: RouteLineHistoryRecordingEnabledObserver

    @Before
    fun setup() {
        every { mockMapboxNavigation.retrieveCompositeHistoryRecorder() } returns compositeRecorder
        every { mockMapboxNavigation.historyRecorder } returns manualRecorder
        every { mockMapboxNavigation.navigationOptions } returns mockk {
            every { copilotOptions } returns mockk {
                every { shouldRecordRouteLineEvents } returns false
            }
        }
        observer = RouteLineHistoryRecordingEnabledObserver(mockMapboxNavigation, recorderObserver)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `onEnabled invoke recorder with composite observer if no handles enabled and copilot enabled`() {
        val mockHandle = mockk<MapboxHistoryRecorder>()
        every { mockMapboxNavigation.navigationOptions } returns mockk {
            every { copilotOptions } returns mockk {
                every { shouldRecordRouteLineEvents } returns true
            }
        }
        observer.onEnabled(mockHandle)
        verify {
            recorderObserver(compositeRecorder)
        }
    }

    @Test
    fun `onEnabled invoke recorder with manual observer if no handles enabled and copilot disabled`() {
        val mockHandle = mockk<MapboxHistoryRecorder>()
        every { mockMapboxNavigation.navigationOptions } returns mockk {
            every { copilotOptions } returns mockk {
                every { shouldRecordRouteLineEvents } returns false
            }
        }
        observer.onEnabled(mockHandle)
        verify {
            recorderObserver(manualRecorder)
        }
    }

    @Test
    fun `onEnabled should not invoke recorder observer if handles already enabled`() {
        val mockHandle1 = mockk<MapboxHistoryRecorder>()
        val mockHandle2 = mockk<MapboxHistoryRecorder>()
        observer.onEnabled(mockHandle1)
        observer.onEnabled(mockHandle2)
        verify(exactly = 1) {
            recorderObserver(any())
        }
    }

    @Test
    fun `onEnabled should not invoke recorder observer if handles already enabled same handle`() {
        val mockHandle = mockk<MapboxHistoryRecorder>()
        observer.onEnabled(mockHandle)
        observer.onEnabled(mockHandle)
        verify(exactly = 1) {
            recorderObserver(any())
        }
    }

    @Test
    fun `onDisabled should invoke observer with null if no handles remaining`() {
        val mockHandle = mockk<MapboxHistoryRecorder>()
        observer.onEnabled(mockHandle)
        observer.onDisabled(mockHandle)
        verify {
            recorderObserver(null)
        }
    }

    @Test
    fun `onDisabled should not invoke recorder observer if handles remaining`() {
        val mockHandle1 = mockk<MapboxHistoryRecorder>()
        val mockHandle2 = mockk<MapboxHistoryRecorder>()
        observer.onEnabled(mockHandle1)
        observer.onEnabled(mockHandle2)
        clearMocks(recorderObserver, answers = false)
        observer.onDisabled(mockHandle1)
        verify(exactly = 0) {
            recorderObserver(any())
        }
    }
}
