package com.mapbox.navigation.core.routerefresh

internal class RetryRouteRefreshStrategy(
    private val maxRetryCount: Int
) {

    private var attemptNumber = 0

    fun reset() {
        attemptNumber = 0
    }

    fun shouldRetry(): Boolean {
        return attemptNumber <= maxRetryCount
    }

    fun onNextAttempt() {
        attemptNumber++
    }
}
