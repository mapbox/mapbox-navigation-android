package com.mapbox.navigation.core.ev

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.reroute.RouteOptionsModifier
import com.mapbox.navigation.core.routeoptions.isEVRoute

internal class EVRouteOptionsModifier(
    private val evDynamicDataHolder: EVDynamicDataHolder
) : RouteOptionsModifier {

    override fun modify(options: RouteOptions): RouteOptions {
        if (!options.isEVRoute()) {
            return options
        }
        val latestEvData = evDynamicDataHolder.currentData(options.unrecognizedJsonProperties!!)
        return options.toBuilder()
            .unrecognizedJsonProperties(
                options.unrecognizedJsonProperties!!.apply {
                    putAll(latestEvData.mapValues { JsonPrimitive(it.value) })
                }
            )
            .build()
    }
}
