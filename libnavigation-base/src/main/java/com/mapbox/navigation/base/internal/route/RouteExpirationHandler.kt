package com.mapbox.navigation.base.internal.route

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.Time

@MainThread
object RouteExpirationHandler {

    private val routesExpirationTimes = mutableMapOf<String, Long>()

    fun updateRouteExpirationData(route: NavigationRoute, refreshTtl: Int?) {
        if (refreshTtl != null) {
            routesExpirationTimes[route.id] = Time.SystemClockImpl.seconds() + refreshTtl
        }
    }

    fun isRouteExpired(route: NavigationRoute): Boolean {
        val routeExpirationTime = routesExpirationTimes[route.id]
        return routeExpirationTime != null && routeExpirationTime <= Time.SystemClockImpl.seconds()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun clear() {
        routesExpirationTimes.clear()
    }
}
