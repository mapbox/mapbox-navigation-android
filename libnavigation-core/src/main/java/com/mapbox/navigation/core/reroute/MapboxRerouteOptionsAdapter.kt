package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRerouteOptionsAdapter

internal class MapboxRerouteOptionsAdapter @VisibleForTesting constructor(
    private val optionsAdapters: List<RerouteOptionsAdapter>
) : RerouteOptionsAdapter {

    constructor(evDynamicDataHolder: EVDynamicDataHolder) : this(
        listOf(EVRerouteOptionsAdapter(evDynamicDataHolder))
    )

    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
        return optionsAdapters.fold(routeOptions) { value, modifier ->
            modifier.onRouteOptions(value)
        }
    }
}
