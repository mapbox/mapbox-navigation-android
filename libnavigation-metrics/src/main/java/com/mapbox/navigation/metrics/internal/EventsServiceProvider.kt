package com.mapbox.navigation.metrics.internal

import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService
import com.mapbox.common.EventsServiceInterface

object EventsServiceProvider {
    fun provideEventsService(eventsServerOptions: EventsServerOptions): EventsServiceInterface =
        EventsService.getOrCreate(eventsServerOptions)
}
