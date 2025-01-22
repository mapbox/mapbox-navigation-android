package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import java.util.concurrent.CopyOnWriteArraySet

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DeveloperMetadataAggregator(
    initialCopilotSessionId: String,
) : CopilotSessionObserver {

    private val observers = CopyOnWriteArraySet<DeveloperMetadataObserver>()
    private var currentMetadata: DeveloperMetadata = DeveloperMetadata(initialCopilotSessionId)
        set(value) {
            if (field != value) {
                field = value
                observers.forEach {
                    it.onDeveloperMetadataChanged(value)
                }
            }
        }

    @UiThread // otherwise double notification with the same value is possible
    fun registerObserver(observer: DeveloperMetadataObserver) {
        observers.add(observer)
        observer.onDeveloperMetadataChanged(currentMetadata)
    }

    fun unregisterObserver(observer: DeveloperMetadataObserver) {
        observers.remove(observer)
    }

    fun unregisterAllObservers() {
        observers.clear()
    }

    override fun onCopilotSessionChanged(session: HistoryRecordingSessionState) {
        currentMetadata = currentMetadata.copy(copilotSessionId = session.sessionId)
    }
}
