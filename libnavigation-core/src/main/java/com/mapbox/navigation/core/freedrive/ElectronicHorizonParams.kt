package com.mapbox.navigation.core.freedrive

class ElectronicHorizonParams private constructor(
        val delay: Long,
        val interval: Long,
        val locationsCacheSize: Int
) {
    class Builder {
        companion object {
            internal const val DEFAULT_ELECTRONIC_HORIZON_DELAY = 20_000L
            internal const val DEFAULT_ELECTRONIC_HORIZON_INTERVAL = 20_000L
            internal const val DEFAULT_LOCATIONS_CACHE_SIZE = 5
            internal const val LOCATIONS_CACHE_MIN_SIZE = 2
            internal const val LOCATIONS_CACHE_MAX_SIZE = 10
        }

        private var delay: Long = DEFAULT_ELECTRONIC_HORIZON_DELAY
        private var interval: Long = DEFAULT_ELECTRONIC_HORIZON_INTERVAL
        private var locationsCacheSize: Int = DEFAULT_LOCATIONS_CACHE_SIZE

        fun delay(delay: Long) = apply {
            invokeIf(delay > 0) { this.delay = delay }
        }

        fun interval(interval: Long) = apply {
            invokeIf(interval > 0) { this.interval = interval }
        }

        fun locationsCacheSize(cacheSize: Int) = apply {
            invokeIf(cacheSize in LOCATIONS_CACHE_MIN_SIZE..LOCATIONS_CACHE_MAX_SIZE) {
                this.locationsCacheSize = cacheSize
            }
        }

        fun build() = ElectronicHorizonParams(
                delay,
                interval,
                locationsCacheSize
        )

        private inline fun invokeIf(condition: Boolean, block: () -> Unit) {
            if (condition) {
                block()
            }
        }
    }
}