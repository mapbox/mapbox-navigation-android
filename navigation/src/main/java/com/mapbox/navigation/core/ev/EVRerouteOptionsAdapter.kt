package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.isEVRoute
import com.mapbox.navigation.core.reroute.InternalRerouteOptionsAdapter
import com.mapbox.navigation.core.reroute.RouteOptionsAdapterParams

internal class EVRerouteOptionsAdapter(
    private val evDynamicDataHolder: EVDynamicDataHolder,
) : InternalRerouteOptionsAdapter {

    override fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions {
        if (!routeOptions.isEVRoute()) {
            return routeOptions
        }
        val latestEvData = HashMap(
            evDynamicDataHolder.currentData(
                routeOptions.unrecognizedJsonProperties ?: emptyMap(),
            ).mapValues { JsonPrimitive(it.value) },
        )
        return routeOptions.toBuilder()
            .unrecognizedJsonProperties(
                routeOptions.unrecognizedJsonProperties?.apply {
                    putAll(latestEvData)
                } ?: latestEvData,
            )
            .build()
    }
}
