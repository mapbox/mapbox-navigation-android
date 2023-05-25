package com.mapbox.navigation.core.internal.location

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.common.location.LiveTrackingClient
import com.mapbox.common.location.LocationError
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import java.util.concurrent.CopyOnWriteArrayList

private val defaultLocationService: LocationService = LocationServiceFactory.locationService()

internal object NavLocationService : LocationService by defaultLocationService {
    private val customLiveTrackingClient = CopyOnWriteArrayList<LiveTrackingClient>()

    override fun getLiveTrackingClient(
        name: String?,
        capabilities: Value?
    ): Expected<LocationError, LiveTrackingClient> {
        return name?.let { name ->
            customLiveTrackingClient.firstOrNull {
                name == it.name
            }?.let {
                ExpectedFactory.createValue(it)
            }
        } ?: defaultLocationService.getLiveTrackingClient(name, capabilities)
    }

    fun addUserLiveTrackingClient(liveTrackingClient: LiveTrackingClient) {
        customLiveTrackingClient.add(liveTrackingClient)
    }
}
