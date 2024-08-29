package com.mapbox.navigation.ui.androidauto.telemetry

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.NavigationCustomEventType
import com.mapbox.navigation.core.internal.telemetry.sendCustomEvent
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxCarTelemetry : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.sendCustomEvent(
            payload = STARTED,
            customEventType = NavigationCustomEventType.ANALYTICS,
            customEventVersion = EVENT_VERSION,
        )
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.sendCustomEvent(
            payload = STOPPED,
            customEventType = NavigationCustomEventType.ANALYTICS,
            customEventVersion = EVENT_VERSION,
        )
    }

    private companion object {
        private const val EVENT_VERSION = "1.0.0"
        private const val STARTED = "Android Auto : started"
        private const val STOPPED = "Android Auto : stopped"
    }
}
