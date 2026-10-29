package com.mapbox.navigation.core.internal.router

import com.mapbox.common.Cancelable
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouteRefreshWithRoute
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterRefreshCallback

/**
 * Refresh of an offline route on the device. Wraps the static native calls of
 * [RouteRefreshWithRoute], which unit tests can't mock.
 */
internal interface OnDeviceRouteRefresh {

    /**
     * Whether [router] refreshes an offline route that requests `state_of_charge` on the device.
     */
    fun isSupported(router: RouterInterface): Boolean

    fun refresh(
        router: RouterInterface,
        route: RouteInterface,
        options: RouteRefreshOptions,
        callback: RouterRefreshCallback,
    ): Cancelable
}

internal object NativeOnDeviceRouteRefresh : OnDeviceRouteRefresh {

    override fun isSupported(router: RouterInterface): Boolean =
        RouteRefreshWithRoute.supportsOnDeviceRefresh(router)

    override fun refresh(
        router: RouterInterface,
        route: RouteInterface,
        options: RouteRefreshOptions,
        callback: RouterRefreshCallback,
    ): Cancelable = RouteRefreshWithRoute.refreshWithRoute(router, route, options, callback)
}
