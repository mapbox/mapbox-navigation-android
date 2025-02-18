package com.mapbox.navigation.mapgpt.core.analytics

fun interface MapGptAnalyticsEventObserver {
    fun onEvent(event: MapGptAnalyticsEvent)
}
