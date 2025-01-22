package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.MapboxHistoryReaderProvider
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Testing and previewing the navigation experience is an important part of building with the
 * Navigation SDK. You can use the SDK's replay functionality to go back in time and replay
 * real experiences.
 *
 * The [ReplayHistorySession] will push the history events into the [MapboxReplayer]. Some
 * customizations can found in the [ReplayHistorySessionOptions]. In order to record history files
 * refer to the [MapboxHistoryRecorder].
 *
 * Typically you would save a history file to a registry, and then select history files to replay
 * in order to improve or demo an experience. But for the sake of example, this is how you would
 * replay an experience that just happened.
 * ```
 * mapboxNavigation.historyRecorder.stopRecording { historyFile ->
 *   historyFile?.let {
 *     replayHistorySession.setHistoryFile(historyFile)
 *     replayHistorySession.onAttached(mapboxNavigation)
 *   }
 * }
 * ```
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplayHistorySession : MapboxNavigationObserver {

    private val optionsFlow = MutableStateFlow(ReplayHistorySessionOptions.Builder().build())
    private var mapboxHistoryReader: MapboxHistoryReader? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var lastHistoryEvent: ReplayEventBase? = null
    private var coroutineScope: CoroutineScope? = null

    private val replayEventsObserver = ReplayEventsObserver { events ->
        if (optionsFlow.value.enableSetRoute) {
            events.filterIsInstance<ReplaySetNavigationRoute>().forEach(::setRoute)
        }
        if (isLastEventPlayed(events)) {
            pushMorePoints()
        }
    }

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     *
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .also { this.coroutineScope = it }
        this.mapboxNavigation = mapboxNavigation
        this.lastHistoryEvent = null
        mapboxNavigation.startReplayTripSession()
        mapboxNavigation.mapboxReplayer.stop()
        mapboxNavigation.mapboxReplayer.registerObserver(replayEventsObserver)
        observeStateFlow(mapboxNavigation).launchIn(coroutineScope)
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     *
     * @param mapboxNavigation
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope?.cancel()
        coroutineScope = null
        this.mapboxNavigation = null
        mapboxNavigation.mapboxReplayer.unregisterObserver(replayEventsObserver)
        mapboxNavigation.mapboxReplayer.stop()
        mapboxNavigation.mapboxReplayer.clearEvents()
    }

    /**
     * Allows you to get or observe the [ReplayHistorySessionOptions].
     */
    fun getOptions(): StateFlow<ReplayHistorySessionOptions> = optionsFlow.asStateFlow()

    /**
     * Update the [ReplayHistorySessionOptions].
     */
    fun setOptions(options: ReplayHistorySessionOptions) {
        optionsFlow.value = options
    }

    /**
     * Change the history file to replay, if changed after [onAttached], the previous trip will
     * be reset and the new file will start.
     */
    fun setHistoryFile(absolutePath: String) {
        optionsFlow.update { it.toBuilder().filePath(absolutePath).build() }
    }

    private fun observeStateFlow(mapboxNavigation: MapboxNavigation): Flow<*> {
        return optionsFlow.mapDistinct { it.filePath }.onEach { historyFile ->
            mapboxNavigation.mapboxReplayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(emptyList())
            mapboxHistoryReader = historyFile?.let { MapboxHistoryReaderProvider.create(it) }
            mapboxNavigation.resetTripSession {
                mapboxNavigation.mapboxReplayer.play()
                pushMorePoints()
            }
        }
    }

    private inline fun <T, R> Flow<T>.mapDistinct(
        crossinline transform: suspend (value: T) -> R,
    ): Flow<R> = map(transform).distinctUntilChanged()

    private fun isLastEventPlayed(events: List<ReplayEventBase>): Boolean {
        val currentEvent = events.lastOrNull() ?: return false
        val lastEventTimestamp = this.lastHistoryEvent?.eventTimestamp ?: 0.0
        return currentEvent.eventTimestamp >= lastEventTimestamp
    }

    private fun pushMorePoints() {
        val mapboxNavigation = mapboxNavigation ?: return
        val replayEvents = mapboxHistoryReader?.takeLocations(TAKE_EVENT_COUNT)
            ?.mapNotNull(::toReplayEvent)
            ?.run { if (isNullOrEmpty()) null else this }
            ?: return
        lastHistoryEvent = replayEvents.lastOrNull()
        mapboxNavigation.mapboxReplayer.clearPlayedEvents()
        mapboxNavigation.mapboxReplayer.pushEvents(replayEvents)
    }

    private fun toReplayEvent(historyEvent: HistoryEvent): ReplayEventBase? =
        optionsFlow.value.replayHistoryMapper.mapToReplayEvent(historyEvent)

    private fun setRoute(replaySetRoute: ReplaySetNavigationRoute) {
        replaySetRoute.route?.let { directionRoute ->
            mapboxNavigation?.setNavigationRoutes(listOf(directionRoute))
        }
    }

    /**
     * Loads the next [count] location events from the history file and returns all of the
     * [HistoryEvent] in a list. The size of the list returned will often be larger than [count]
     * because there are other types of events in the history file.
     *
     * @param count the maximum number of [HistoryEventUpdateLocation] to take
     */
    private fun MapboxHistoryReader.takeLocations(count: Int): List<HistoryEvent> {
        val historyEvents = mutableListOf<HistoryEvent>()
        var locationCount = 0
        while (locationCount < count && hasNext()) {
            val event = next()
            if (event is HistoryEventUpdateLocation) {
                locationCount++
            }
            historyEvents.add(event)
        }
        return historyEvents
    }

    private companion object {

        /**
         * Number of events to read from the history reader at a time.
         */
        private const val TAKE_EVENT_COUNT = 10
    }
}
