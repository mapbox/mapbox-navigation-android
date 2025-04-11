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

    internal object NotificationSubtype {
        private const val EV_MIN_CHARGE_AT_CHARGING_STATION = "evMinChargeAtChargingStation"
        private const val EV_MIN_CHARGE_AT_DESTINATION = "evMinChargeAtDestination"
        private const val EV_INSUFFICIENT_CHARGE = "evInsufficientCharge"
        private const val EV_STATION_UNAVAILABLE = "stationUnavailable"

        val EV_NOTIFICATIONS_SUB_TYPES = setOf(
            EV_MIN_CHARGE_AT_CHARGING_STATION,
            EV_MIN_CHARGE_AT_DESTINATION,
            EV_INSUFFICIENT_CHARGE,
            EV_STATION_UNAVAILABLE,
        )
    }
}
