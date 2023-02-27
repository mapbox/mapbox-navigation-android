package com.mapbox.navigation.metrics.events

import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService

object TelemetryEventsProvider {
    fun getOrCreateTelemetryEventsManager(accessToken: String): EventsServiceInterfacesManager =
        EventsServiceInterfacesManager(
            EventsService.getOrCreate(
                EventsServerOptions(accessToken, "MapboxNavigationNative", null)
            )
        )
}
