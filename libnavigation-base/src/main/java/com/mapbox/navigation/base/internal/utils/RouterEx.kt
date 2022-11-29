package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouterOrigin

fun RouterOrigin.mapToSdkRouteOrigin(): com.mapbox.navigation.base.route.RouterOrigin =
    when (this) {
        RouterOrigin.ONLINE -> com.mapbox.navigation.base.route.RouterOrigin.Offboard
        RouterOrigin.ONBOARD -> com.mapbox.navigation.base.route.RouterOrigin.Onboard
        RouterOrigin.CUSTOM -> com.mapbox.navigation.base.route.RouterOrigin.Custom()
    }

fun com.mapbox.navigation.base.route.RouterOrigin.mapToNativeRouteOrigin(): RouterOrigin =
    when (this) {
        com.mapbox.navigation.base.route.RouterOrigin.Offboard -> RouterOrigin.ONLINE
        com.mapbox.navigation.base.route.RouterOrigin.Onboard -> RouterOrigin.ONBOARD
        is com.mapbox.navigation.base.route.RouterOrigin.Custom -> RouterOrigin.CUSTOM
    }

fun NavigationRoute.internalWaypoints(): List<Waypoint> = nativeWaypoints
