package com.mapbox.services.android.navigation.v5.internal.navigation

data class ElectronicHorizonParams @JvmOverloads constructor(
    val delay: Long = ELECTRONIC_HORIZON_DELAY,
    val interval: Long = ELECTRONIC_HORIZON_INTERVAL,
    val locationsCacheSize: Int = LOCATIONS_CACHE_MAX_SIZE
) {
    companion object {
        private const val ELECTRONIC_HORIZON_DELAY = 20_000L
        private const val ELECTRONIC_HORIZON_INTERVAL = 20_000L
        private const val LOCATIONS_CACHE_MAX_SIZE = 5
    }
}
