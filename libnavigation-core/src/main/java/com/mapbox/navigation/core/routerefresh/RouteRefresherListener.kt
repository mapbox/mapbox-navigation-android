package com.mapbox.navigation.core.routerefresh

internal fun interface RouteRefresherListener {
    fun onRoutesRefreshed(result: RouteRefresherResult)
}
