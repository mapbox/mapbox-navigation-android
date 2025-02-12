@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterOrigin

val RouterError.isErrorRetryable: Boolean
    get() = type == RouterErrorType.NETWORK_ERROR

@com.mapbox.navigation.base.route.RouterOrigin
fun RouterOrigin.mapToSdkRouteOrigin(): String =
    when (this) {
        RouterOrigin.ONLINE -> com.mapbox.navigation.base.route.RouterOrigin.ONLINE
        RouterOrigin.ONBOARD -> com.mapbox.navigation.base.route.RouterOrigin.OFFLINE
        RouterOrigin.CUSTOM_EXTERNAL ->
            com.mapbox.navigation.base.route.RouterOrigin.CUSTOM_EXTERNAL
        RouterOrigin.CUSTOM -> error("native CUSTOM origin isn't supported")
    }

fun @receiver:com.mapbox.navigation.base.route.RouterOrigin String.mapToNativeRouteOrigin():
    RouterOrigin =
    when (this) {
        com.mapbox.navigation.base.route.RouterOrigin.ONLINE -> RouterOrigin.ONLINE
        com.mapbox.navigation.base.route.RouterOrigin.OFFLINE -> RouterOrigin.ONBOARD
        com.mapbox.navigation.base.route.RouterOrigin.CUSTOM_EXTERNAL ->
            RouterOrigin.CUSTOM_EXTERNAL
        else -> error("$this origin isn't supported")
    }

fun NavigationRoute.internalWaypoints(): List<Waypoint> = nativeWaypoints
