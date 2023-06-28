package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Provides components needed for base router capabilities.
 */
@ExperimentalMapboxNavigationAPI
object RouterFactory {

    /**
     * Build a refresh error object needed for [NavigationRouterRefreshCallback].
     */
    @JvmOverloads
    fun buildNavigationRouterRefreshError(
        message: String? = null,
        throwable: Throwable? = null,
        routerFailure: RouterFailure? = null,
        refreshTtl: Int? = null,
    ) = NavigationRouterRefreshError(message, throwable, routerFailure, refreshTtl)
}
