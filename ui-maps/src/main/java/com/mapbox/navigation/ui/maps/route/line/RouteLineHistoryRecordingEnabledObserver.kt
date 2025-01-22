package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.HistoryRecordingEnabledObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCompositeHistoryRecorder

internal class RouteLineHistoryRecordingEnabledObserver(
    private val mapboxNavigation: MapboxNavigation,
    private val compositeHistoryRecorderChangedObserver: (MapboxHistoryRecorder?) -> Unit,
) : HistoryRecordingEnabledObserver {

    private var enabledHandles = mutableSetOf<MapboxHistoryRecorder>()

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder) {
        val shouldStart = enabledHandles.isEmpty()
        enabledHandles.add(historyRecorderHandle)
        if (shouldStart) {
            val recorder = if (
                mapboxNavigation.navigationOptions.copilotOptions.shouldRecordRouteLineEvents
            ) {
                mapboxNavigation.retrieveCompositeHistoryRecorder()
            } else {
                mapboxNavigation.historyRecorder
            }
            compositeHistoryRecorderChangedObserver(recorder)
        }
    }

    override fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder) {
        enabledHandles.remove(historyRecorderHandle)
        if (enabledHandles.isEmpty()) {
            compositeHistoryRecorderChangedObserver(null)
        }
    }
}
