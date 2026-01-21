package com.mapbox.navigation.core.routerefresh

/**
 * Callback invoked when an immediate route refresh request finishes.
 */
fun interface ImmediateRouteRefreshCallback {
    /**
     * Called when the refresh request is finished.
     *
     * @param result the result of the refresh request
     */
    fun onRefreshFinished(result: ImmediateRouteRefreshResult)
}
