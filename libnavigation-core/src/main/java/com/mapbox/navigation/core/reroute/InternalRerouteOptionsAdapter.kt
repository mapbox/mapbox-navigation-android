package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.internal.router.GetRouteSignature

/**
 * Similar to [RerouteOptionsAdapter], but we can break its api as it's not public
 */
internal interface InternalRerouteOptionsAdapter {
    fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions
}

internal data class RouteOptionsAdapterParams(
    val signature: GetRouteSignature,
)
