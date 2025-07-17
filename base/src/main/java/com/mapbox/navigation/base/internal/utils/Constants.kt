package com.mapbox.navigation.base.internal.utils

class Constants {
    object RouteResponse {
        const val KEY_WAYPOINTS = "waypoints"
        const val KEY_REFRESH_TTL = "refresh_ttl"
        const val KEY_NOTIFICATIONS = "notifications"
    }

    object CongestionRange {
        val LOW_CONGESTION_RANGE = 0..39
        val MODERATE_CONGESTION_RANGE = 40..59
        val HEAVY_CONGESTION_RANGE = 60..79
        val SEVERE_CONGESTION_RANGE = 80..100
    }

    internal object NotificationRefreshType {
        const val STATIC = "static"
        const val DYNAMIC = "dynamic"
    }
}
