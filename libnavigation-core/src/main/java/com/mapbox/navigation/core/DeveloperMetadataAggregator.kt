package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DeveloperMetadataAggregator(
    copilotSessionIdFlow: SharedFlow<String>,
    mainScope: CoroutineScope,
) {

    private val metadata: MutableSharedFlow<DeveloperMetadata> = MutableSharedFlow(
        replay = 1,
    )
    private val observers = CopyOnWriteArraySet<DeveloperMetadataObserver>()

    init {
        mainScope.launch {
            copilotSessionIdFlow.collect {
                metadata.emit(DeveloperMetadata(it))
            }
        }
        mainScope.launch {
            metadata.collect {
                observers.forEach { observer ->
                    observer.onDeveloperMetadataChanged(it)
                }
            }
        }
    }

    @UiThread // otherwise double notification with the same value is possible
    fun registerObserver(observer: DeveloperMetadataObserver) {
        observers.add(observer)
        metadata.replayCache.lastOrNull()?.let { observer.onDeveloperMetadataChanged(it) }
    }

    fun unregisterObserver(observer: DeveloperMetadataObserver) {
        observers.remove(observer)
    }

    fun unregisterAllObservers() {
        observers.clear()
    }
}
