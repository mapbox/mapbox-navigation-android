package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class DeveloperMetadataAggregatorTest {

    private val initialId = "123-123"
    private val observer = mockk<DeveloperMetadataObserver>(relaxed = true)
    private val sut = DeveloperMetadataAggregator(initialId)

    @Test
    fun `observer receives initial value`() {
        sut.registerObserver(observer)

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(initialId))
        }
    }

    @Test
    fun `observer receives changed value`() {
        val newValue = "456-654"

        sut.registerObserver(observer)
        clearMocks(observer)
        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = newValue))

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }
    }

    @Test
    fun `observer does not receive same value`() {
        val newValue = "456-654"

        sut.registerObserver(observer)
        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = newValue))
        clearMocks(observer)
        sut.onCopilotSessionChanged(
            HistoryRecordingSessionState.ActiveGuidance(sessionId = newValue),
        )

        verify(exactly = 0) { observer.onDeveloperMetadataChanged(any()) }
    }

    @Test
    fun `observer does not receive changed value after unregister`() {
        val newValue = "456-654"
        sut.registerObserver(observer)
        clearMocks(observer)

        sut.unregisterObserver(observer)
        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = newValue))

        verify(exactly = 0) { observer.onDeveloperMetadataChanged(any()) }
    }

    @Test
    fun unregisterAllObservers() {
        val newValue = "456-654"
        val secondObserver = mockk<DeveloperMetadataObserver>(relaxed = true)
        sut.registerObserver(observer)
        sut.registerObserver(secondObserver)
        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = newValue))

        verify(exactly = 1) {
            observer.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }
        verify(exactly = 1) {
            secondObserver.onDeveloperMetadataChanged(DeveloperMetadata(newValue))
        }

        sut.unregisterAllObservers()
        clearMocks(observer, secondObserver)
        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = "789-987"))

        verify(exactly = 0) {
            observer.onDeveloperMetadataChanged(any())
        }
        verify(exactly = 0) {
            secondObserver.onDeveloperMetadataChanged(any())
        }
    }

    @Test
    fun `observer unregisters itself from callback`() {
        val newId = "456-654"
        val observer = object : DeveloperMetadataObserver {
            override fun onDeveloperMetadataChanged(metadata: DeveloperMetadata) {
                if (metadata.copilotSessionId == newId) {
                    sut.unregisterObserver(this)
                }
            }
        }
        sut.registerObserver(observer)

        sut.onCopilotSessionChanged(HistoryRecordingSessionState.FreeDrive(sessionId = newId))
        // verify no crash
    }
}
