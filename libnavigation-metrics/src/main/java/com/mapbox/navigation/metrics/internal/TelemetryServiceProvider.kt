package com.mapbox.navigation.metrics.internal

import com.mapbox.common.EventsServerOptions
import com.mapbox.common.TelemetryService

object TelemetryServiceProvider {

    fun provideTelemetryService(
        eventsServerOptions: EventsServerOptions
    ): TelemetryService = TelemetryService.getOrCreate(eventsServerOptions)
}
