package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.reroute.internal.NativeRerouteControllerState
import com.mapbox.navigation.utils.internal.logW

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal fun NativeRerouteControllerState.toRerouteStateV2(): RerouteStateV2 {
    return when (this) {
        is NativeRerouteControllerState.Idle -> RerouteStateV2.Idle()
        is NativeRerouteControllerState.Interrupted -> RerouteStateV2.Interrupted()
        is NativeRerouteControllerState.Failed -> RerouteStateV2.Failed(
            message,
            throwable,
            reasons,
            preRouterReasons,
        )
        is NativeRerouteControllerState.WaitingForResponse -> RerouteStateV2.FetchingRoute()
        is NativeRerouteControllerState.RouteObjectsParsing -> RerouteStateV2.FetchingRoute()
        is NativeRerouteControllerState.RouteFetched -> RerouteStateV2.RouteFetched(routerOrigin)
        is NativeRerouteControllerState.Deviation.ApplyingRoute ->
            RerouteStateV2.Deviation.ApplyingRoute()
        is NativeRerouteControllerState.Deviation.RouteIgnored ->
            RerouteStateV2.Deviation.RouteIgnored()
    }
}

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
