package com.mapbox.navigation.core.routerefresh

internal fun interface RouteRefresherListener {
    fun onRoutesRefreshed(result: RoutesRefresherResult)
}

internal fun interface RoutesRefreshAttemptListener {

    fun onRoutesRefreshAttemptFinished(result: RoutesRefresherResult)
}
