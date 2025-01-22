package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DelayedRoutesRenderedCallbackTest {

    private val originalCallback = mockk<RoutesRenderedCallback>(relaxed = true)
    private val delayedCallback = DelayedRoutesRenderedCallback(originalCallback)
    private val result = mockk<RoutesRenderedResult>()

    @Test
    fun onResultBeforeUnlock() {
        delayedCallback.onRoutesRendered(result)

        verify(exactly = 0) {
            originalCallback.onRoutesRendered(any())
        }

        delayedCallback.unlock()
        verify(exactly = 1) {
            originalCallback.onRoutesRendered(result)
        }

        clearAllMocks(answers = false)
        delayedCallback.unlock()
        verify(exactly = 0) {
            originalCallback.onRoutesRendered(any())
        }
    }

    @Test
    fun onResultAfterUnlock() {
        delayedCallback.unlock()
        verify(exactly = 0) {
            originalCallback.onRoutesRendered(any())
        }

        delayedCallback.onRoutesRendered(result)
        verify(exactly = 1) {
            originalCallback.onRoutesRendered(result)
        }
    }
}
