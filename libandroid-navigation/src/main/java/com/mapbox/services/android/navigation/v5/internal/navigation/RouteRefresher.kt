package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.RouteRefresh
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import java.util.Date

internal class RouteRefresher(
    private val mapboxNavigation: MapboxNavigation,
    private val routeRefresh: RouteRefresh
) {
    private val refreshIntervalInMilliseconds: Long = mapboxNavigation.options().refreshIntervalInMilliseconds()
    private val isRefreshRouteEnabled: Boolean = mapboxNavigation.options().enableRefreshRoute()
    private var lastRefreshedDate = Date()
    private var isChecking: Boolean = false

    fun check(currentDate: Date): Boolean {
        if (isChecking || !isRefreshRouteEnabled) {
            return false
        }
        val millisSinceLastRefresh = currentDate.time - lastRefreshedDate.time
        return millisSinceLastRefresh > refreshIntervalInMilliseconds
    }

    fun refresh(routeProgress: RouteProgress) {
        updateIsChecking(true)
        routeRefresh.refresh(routeProgress, RouteRefresherCallback(mapboxNavigation, this))
    }

    fun updateLastRefresh(date: Date) {
        lastRefreshedDate = date
    }

    fun updateIsChecking(isChecking: Boolean) {
        this.isChecking = isChecking
    }
}
