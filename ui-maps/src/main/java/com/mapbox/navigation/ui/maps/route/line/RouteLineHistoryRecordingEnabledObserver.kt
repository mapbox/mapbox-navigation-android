package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.HistoryRecordingEnabledObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCompositeHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder

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
            val options = mapboxNavigation.navigationOptions
            val bothRecordersEnabled = options.copilotOptions.shouldRecordRouteLineEvents &&
                options.historyRecorderOptions.shouldRecordRouteLineEvents
            val recorder = when {
                bothRecordersEnabled -> mapboxNavigation.retrieveCompositeHistoryRecorder()
                options.copilotOptions.shouldRecordRouteLineEvents ->
                    mapboxNavigation.retrieveCopilotHistoryRecorder()
                options.historyRecorderOptions.shouldRecordRouteLineEvents ->
                    mapboxNavigation.historyRecorder
                else -> null
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
