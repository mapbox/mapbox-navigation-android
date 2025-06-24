package com.mapbox.navigation.ui.maps.route.line

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.registerObserver
import com.mapbox.navigation.core.internal.history.HistoryRecorderChooser
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineEvent
import com.mapbox.navigation.ui.maps.util.LimitedQueue
import com.mapbox.navigation.ui.maps.util.MutexBasedScope
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val HISTORY_RECORDING_QUEUE_SIZE = 8

internal object RouteLineHistoryRecordingPusherProvider {
    val instance: RouteLineHistoryRecordingPusher = RouteLineHistoryRecordingPusher()
}

@VisibleForTesting
internal interface HistoryRecorderChooserFactory {

    fun create(
        mapboxNavigation: MapboxNavigation,
        listener: (MapboxHistoryRecorder?) -> Unit,
    ): HistoryRecorderChooser
}

internal class DefaultHistoryRecorderChooserFactory : HistoryRecorderChooserFactory {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun create(
        mapboxNavigation: MapboxNavigation,
        listener: (MapboxHistoryRecorder?) -> Unit,
    ): HistoryRecorderChooser {
        val options = mapboxNavigation.navigationOptions
        return HistoryRecorderChooser(
            historyEnabled = options.historyRecorderOptions.shouldRecordRouteLineEvents,
            copilotEnabled = options.copilotOptions.shouldRecordRouteLineEvents,
            mapboxNavigation,
            listener,
        )
    }
}

@VisibleForTesting
internal class RouteLineHistoryRecordingPusher(
    private val serialisationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mutexBasedMainScope: MutexBasedScope = MutexBasedScope(
        InternalJobControlFactory.createImmediateMainScopeJobControl().scope,
    ),
    private val historyRecorderChooserFactory: HistoryRecorderChooserFactory =
        DefaultHistoryRecorderChooserFactory(),
) : MapboxNavigationObserver {

    private var recorder: MapboxHistoryRecorder? = null
        set(value) {
            field = value
            if (value != null) {
                eventsQueue.forEach { pushEvent(value, it) }
                eventsQueue.clear()
            } else {
                mutexBasedMainScope.cancelChildren()
            }
        }

    private val eventsQueue =
        LimitedQueue<suspend (CoroutineContext) -> RouteLineEvent>(HISTORY_RECORDING_QUEUE_SIZE)
    private var historyRecordingEnabledObserver: HistoryRecorderChooser? = null

    init {
        MapboxNavigationProvider.registerObserver(this)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        historyRecordingEnabledObserver = historyRecorderChooserFactory.create(
            mapboxNavigation,
            { recorder = it },
        )
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        historyRecordingEnabledObserver?.destroy()
        historyRecordingEnabledObserver = null
        recorder = null
    }

    fun pushEventOrAddToQueue(
        @WorkerThread eventFormer: suspend (CoroutineContext) -> RouteLineEvent,
    ) {
        val recorderCopy = recorder
        if (recorderCopy == null) {
            eventsQueue.add(eventFormer)
        } else {
            pushEvent(recorderCopy, eventFormer)
        }
    }

    private fun pushEvent(
        recorder: MapboxHistoryRecorder,
        @WorkerThread eventFormer: suspend (CoroutineContext) -> RouteLineEvent,
    ) {
        mutexBasedMainScope.launchWithMutex {
            val eventJson = withContext(serialisationDispatcher) {
                try {
                    eventFormer(serialisationDispatcher).toJson()
                } catch (ex: Throwable) {
                    ensureActive()
                    null
                }
            }
            if (eventJson != null) {
                recorder.pushHistory("mbx.RouteLine", eventJson)
            }
        }
    }

    fun pushEventIfEnabled(
        @WorkerThread
        eventFormer: suspend (CoroutineContext) -> RouteLineEvent,
    ) {
        recorder?.let { pushEvent(it, eventFormer) }
    }
}
