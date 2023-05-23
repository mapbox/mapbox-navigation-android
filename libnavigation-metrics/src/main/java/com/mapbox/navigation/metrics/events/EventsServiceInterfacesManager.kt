package com.mapbox.navigation.metrics.events

import com.mapbox.bindgen.Value
import com.mapbox.common.EventsServiceError
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.EventsServiceObserver

class EventsServiceInterfacesManager(
//    platformEventsServiceInterface: EventsServiceInterface,
    internal val nativeEventsServiceInterface: EventsServiceInterface,
) {
    private val eventServices = listOf(
//        platformEventsServiceInterface,
        nativeEventsServiceInterface,
    )

    private val observers = mutableListOf<EventsObserver>()

    private val eventsServiceObserver = object : EventsServiceObserver {
        override fun didEncounterError(error: EventsServiceError, events: Value) {
            // do nothing
        }

        override fun didSendEvents(events: Value) {
            observers.forEach { it.onEvents(events) }
        }
    }

    fun registerEventsObserver(eventsObserver: EventsObserver) {
        checkAndRegisterInternal()
        observers.add(eventsObserver)
    }

    fun unregisterEventsObserver(eventsObserver: EventsObserver) {
        observers.remove(eventsObserver)
        checkAndUnregisterInternal()
    }

    private fun checkAndRegisterInternal() {
        if (observers.isEmpty()) {
            eventServices.forEach { it.registerObserver(eventsServiceObserver) }
        }
    }

    private fun checkAndUnregisterInternal() {
        if (observers.isEmpty()) {
            eventServices.forEach { it.unregisterObserver(eventsServiceObserver) }
        }
    }
}
