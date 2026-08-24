package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.core.internal.router.GetRouteSignature

internal val defaultRouteOptionsAdapterParams get() = RouteOptionsAdapterParams(deviationSignature)

internal fun createRouteOptionsAdapterParams(
    signature: GetRouteSignature = deviationSignature,
) =
    RouteOptionsAdapterParams(
        signature = signature,
    )
