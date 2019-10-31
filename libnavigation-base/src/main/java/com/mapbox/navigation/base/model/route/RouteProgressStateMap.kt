package com.mapbox.navigation.base.model.route

import java.util.HashMap

class RouteProgressStateMap : HashMap<RouteState, RouteProgressState?>() {
    init {
        put(RouteState.INVALID, RouteProgressState.ROUTE_INVALID)
        put(RouteState.INITIALIZED, RouteProgressState.ROUTE_INITIALIZED)
        put(RouteState.COMPLETE, RouteProgressState.ROUTE_ARRIVED)
        put(RouteState.TRACKING, RouteProgressState.LOCATION_TRACKING)
        put(RouteState.STALE, RouteProgressState.LOCATION_STALE)
        put(RouteState.OFFROUTE, null) // Ignore off-route (info already provided via listener)
    }
}
