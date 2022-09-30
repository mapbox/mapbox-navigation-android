package com.mapbox.navigation.dropin.analytics

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.NavigationCustomEventType
import com.mapbox.navigation.core.internal.telemetry.sendCustomEvent
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AnalyticsComponent : UIComponent() {

    private companion object {
        private const val STARTED = "Drop-In UI : started"
        private const val STOPPED = "Drop-In UI : stopped"
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.sendCustomEvent(
            payload = STARTED,
            customEventType = NavigationCustomEventType.ANALYTICS,
            customEventVersion = "1.0.0"
        )
        super.onAttached(mapboxNavigation)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.sendCustomEvent(
            payload = STOPPED,
            customEventType = NavigationCustomEventType.ANALYTICS,
            customEventVersion = "1.0.0"
        )
        super.onDetached(mapboxNavigation)
    }
}
