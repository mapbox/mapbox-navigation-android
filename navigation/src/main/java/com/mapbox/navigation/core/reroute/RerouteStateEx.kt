package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logW

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal fun RerouteStateV2.toRerouteState(): RerouteState? {
    return when (this) {
        is RerouteStateV2.Idle -> RerouteState.Idle
        is RerouteStateV2.FetchingRoute -> RerouteState.FetchingRoute
        is RerouteStateV2.Failed -> RerouteState.Failed(
            message,
            throwable,
            reasons,
            preRouterReasons,
        )
        is RerouteStateV2.RouteFetched -> RerouteState.RouteFetched(routerOrigin)
        is RerouteStateV2.Interrupted -> RerouteState.Interrupted
        is RerouteStateV2.Deviation.ApplyingRoute -> null
        is RerouteStateV2.Deviation.RouteIgnored -> null
        else -> {
            logW("Unexpected state: $this")
            null
        }
    }
}
