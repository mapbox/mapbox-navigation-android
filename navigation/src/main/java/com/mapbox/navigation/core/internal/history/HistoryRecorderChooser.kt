package com.mapbox.navigation.core.internal.history

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.retrieveCompositeHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder
import com.mapbox.navigation.utils.internal.logI

private enum class HistoryRecorderType { MANUAL, COPILOT }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class HistoryRecorderChooser(
    private val historyEnabled: Boolean,
    private val copilotEnabled: Boolean,
    private val mapboxNavigation: MapboxNavigation,
    private val historyRecorderChangedObserver: (MapboxHistoryRecorder?) -> Unit,
) {

    private val copilotObserver = object : HistoryRecordingEnabledObserver {
        override fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder) {
            enabledHandles.add(HistoryRecorderType.COPILOT)
            onRecorderChanged()
        }

        override fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder) {
            enabledHandles.remove(HistoryRecorderType.COPILOT)
            onRecorderChanged()
        }
    }
    private val historyObserver = object : HistoryRecordingEnabledObserver {
        override fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder) {
            enabledHandles.add(HistoryRecorderType.MANUAL)
            onRecorderChanged()
        }

        override fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder) {
            enabledHandles.remove(HistoryRecorderType.MANUAL)
            onRecorderChanged()
        }
    }

    private var enabledHandles = mutableSetOf<HistoryRecorderType>()

    init {
        if (copilotEnabled) {
            mapboxNavigation.retrieveCopilotHistoryRecorder()
                .registerHistoryRecordingEnabledObserver(copilotObserver)
        }
        if (historyEnabled) {
            mapboxNavigation.historyRecorder
                .registerHistoryRecordingEnabledObserver(historyObserver)
        }
    }

    fun destroy() {
        if (copilotEnabled) {
            mapboxNavigation.retrieveCopilotHistoryRecorder()
                .unregisterHistoryRecordingEnabledObserver(copilotObserver)
        }
        if (historyEnabled) {
            mapboxNavigation.historyRecorder
                .unregisterHistoryRecordingEnabledObserver(historyObserver)
        }
    }

    private fun onRecorderChanged() {
        logI(TAG) { "Recorder changed, enabled recorders: $enabledHandles" }
        val recorder = when {
            enabledHandles.isEmpty() -> null
            enabledHandles.size == 1 -> {
                when (enabledHandles.first()) {
                    HistoryRecorderType.COPILOT -> mapboxNavigation.retrieveCopilotHistoryRecorder()
                    HistoryRecorderType.MANUAL -> mapboxNavigation.historyRecorder
                }
            }
            enabledHandles.size == 2 -> mapboxNavigation.retrieveCompositeHistoryRecorder()
            else -> throw IllegalStateException("Too many recorders enabled: $enabledHandles")
        }
        historyRecorderChangedObserver(recorder)
    }

    private companion object {
        private const val TAG = "HistoryRecorderChooser"
    }
}
