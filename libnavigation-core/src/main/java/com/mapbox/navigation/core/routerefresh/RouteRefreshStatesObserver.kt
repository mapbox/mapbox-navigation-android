package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Route refresh state observer.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface RouteRefreshStatesObserver {

    /**
     * Invoked for the current and every new state.
     */
    fun onNewState(result: RouteRefreshStateResult)
}
