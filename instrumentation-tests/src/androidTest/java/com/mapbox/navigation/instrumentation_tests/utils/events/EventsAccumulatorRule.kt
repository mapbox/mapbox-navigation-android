package com.mapbox.navigation.instrumentation_tests.utils.events

import com.mapbox.bindgen.Value
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService
import com.mapbox.common.EventsServiceError
import com.mapbox.common.EventsServiceObserver
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.Navigator
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class EventsAccumulatorRule(
    mapboxToken: String,
) : TestWatcher() {

    private val eventService = EventsService.getOrCreate(
        EventsServerOptions(
            mapboxToken, Navigator.getUserAgentFragment(), null
        )
    )

    private val eventsServiceObserver = object : EventsServiceObserver {
        override fun didEncounterError(error: EventsServiceError, events: Value) {
            logE(LOG_CATEGORY) {
                "Occurred error [$error] when send events: $events"
            }
        }

        override fun didSendEvents(events: Value) {
            _events.addAll(events.contents as List<Value>)
        }
    }

    private val _events = mutableListOf<Value>()
    val events: List<Value> get() = _events

    private companion object {
        private const val LOG_CATEGORY = "EventsAccumulatorRule"
    }

    override fun starting(description: Description?) {
        _events.clear()
        eventService.registerObserver(eventsServiceObserver)
    }

    override fun finished(description: Description?) {
        eventService.unregisterObserver(eventsServiceObserver)
    }
}
