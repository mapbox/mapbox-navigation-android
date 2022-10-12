package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NavigationComponentProviderTest {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `create DeveloperMetadataAggregator`() {
        val id = "123-123"
        val historyRecordingStateHandler = mockk<HistoryRecordingStateHandler>(relaxed = true) {
            every { currentCopilotSession() } returns NavigationSessionState.FreeDrive(id)
        }
        val observer = mockk<DeveloperMetadataObserver>(relaxed = true)

        val aggregator = NavigationComponentProvider.createDeveloperMetadataAggregator(
            historyRecordingStateHandler
        )
        verify {
            historyRecordingStateHandler.registerCopilotSessionObserver(aggregator)
        }

        aggregator.registerObserver(observer)
        verify { observer.onDeveloperMetadataChanged(DeveloperMetadata(id)) }
    }
}
