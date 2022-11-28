package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.reroute.RerouteOptionsAdapter
import com.mapbox.navigation.core.routeoptions.isEVRoute

internal class EVRerouteOptionsAdapter(
    private val evDynamicDataHolder: EVDynamicDataHolder
) : RerouteOptionsAdapter {

    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
        if (!routeOptions.isEVRoute()) {
            return routeOptions
        }
        val latestEvData = HashMap(
            evDynamicDataHolder.currentData(
                routeOptions.unrecognizedJsonProperties ?: emptyMap()
            ).mapValues { JsonPrimitive(it.value) }
        )
        return routeOptions.toBuilder()
            .unrecognizedJsonProperties(
                routeOptions.unrecognizedJsonProperties?.apply {
                    putAll(latestEvData)
                } ?: latestEvData
            )
            .build()
    }
}
