package com.mapbox.navigation.metrics.events

import com.mapbox.bindgen.Value

fun interface EventsObserver {
    fun onEvents(events: Value)
}
