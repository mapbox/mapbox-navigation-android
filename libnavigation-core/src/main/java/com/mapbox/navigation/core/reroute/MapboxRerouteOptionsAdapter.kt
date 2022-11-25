package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRouteOptionsModifier

internal class MapboxRerouteOptionsAdapter @VisibleForTesting constructor(
    private val optionsModifiers: List<RouteOptionsModifier>
) : RerouteOptionsAdapter {

    constructor(evDynamicDataHolder: EVDynamicDataHolder) : this(
        listOf(EVRouteOptionsModifier(evDynamicDataHolder))
    )

    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
        return optionsModifiers.fold(routeOptions) { value, modifier ->
            modifier.modify(value)
        }
    }
}
