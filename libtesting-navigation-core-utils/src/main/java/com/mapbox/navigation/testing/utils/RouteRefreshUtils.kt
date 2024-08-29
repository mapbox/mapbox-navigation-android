package com.mapbox.navigation.testing.utils

import com.mapbox.navigation.base.route.RouteRefreshOptions

fun RouteRefreshOptions.setTestRouteRefreshInterval(refreshInterval: Long) {
    RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
        isAccessible = true
        set(this@setTestRouteRefreshInterval, refreshInterval)
    }
}
