package com.mapbox.navigation.metrics.events

import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService

object TelemetryEventsProvider {
    fun getOrCreateEventsServiceInterfacesManager(
        accessToken: String,
        userAgent: String,
    ): EventsServiceInterfacesManager =
        EventsServiceInterfacesManager(
            EventsService.getOrCreate(
                EventsServerOptions(accessToken, userAgent, null)
            )
        )
}
