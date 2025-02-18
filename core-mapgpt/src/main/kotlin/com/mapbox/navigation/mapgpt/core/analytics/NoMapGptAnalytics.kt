package com.mapbox.navigation.mapgpt.core.analytics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class NoMapGptAnalytics() : MapGptAnalytics {
    private val _events = MutableSharedFlow<MapGptAnalyticsEvent>()
    override val events: SharedFlow<MapGptAnalyticsEvent> = _events

    override fun registerObserver(observer: MapGptAnalyticsEventObserver) {
        // no-op
    }

    override fun unregisterObserver(observer: MapGptAnalyticsEventObserver) {
        // no-op
    }

    override fun logEvent(event: MapGptAnalyticsEvent) {
        // no-op
    }
}
