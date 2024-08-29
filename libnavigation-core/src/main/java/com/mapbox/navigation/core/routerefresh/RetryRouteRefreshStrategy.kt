package com.mapbox.navigation.core.routerefresh

internal class RetryRouteRefreshStrategy(
    private val maxAttemptsCount: Int,
) {

    private var attemptNumber = 0

    fun reset() {
        attemptNumber = 0
    }

    fun shouldRetry(): Boolean {
        return attemptNumber < maxAttemptsCount
    }

    fun onNextAttempt() {
        attemptNumber++
    }
}
