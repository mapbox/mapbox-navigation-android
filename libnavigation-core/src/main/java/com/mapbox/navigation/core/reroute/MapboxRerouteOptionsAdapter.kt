package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRerouteOptionsAdapter

internal class MapboxRerouteOptionsAdapter @VisibleForTesting constructor(
    private val internalOptionsAdapters: List<InternalRerouteOptionsAdapter>,
) : InternalRerouteOptionsAdapter {

    var externalOptionsAdapter: RerouteOptionsAdapter? = null

    constructor(
        evDynamicDataHolder: EVDynamicDataHolder,
        routeHistoryOptionsAdapter: RouteHistoryOptionsAdapter,
        reasonOptionsAdapter: RerouteContextReasonOptionsAdapter,
        cleanupCARelatedParamsAdapter: CleanupCARelatedParamsAdapter,
    ) : this(
        listOf(
            EVRerouteOptionsAdapter(evDynamicDataHolder),
            routeHistoryOptionsAdapter,
            reasonOptionsAdapter,
            cleanupCARelatedParamsAdapter,
        ),
    )

    override fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions {
        val internalOptions = internalOptionsAdapters.fold(routeOptions) { value, modifier ->
            modifier.onRouteOptions(value, params)
        }
        return externalOptionsAdapter?.onRouteOptions(internalOptions) ?: internalOptions
    }
}
