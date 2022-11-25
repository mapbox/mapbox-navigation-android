package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.utils.internal.Time

internal class RouteRefresherResultProcessor(
    private val observersManager: RefreshObserversManager,
    private val expiringDataRemover: ExpiringDataRemover,
    private val timeProvider: Time,
    private val staleDataTimeoutMillis: Long,
) : RouteRefresherListener {

    private var lastRefreshTimeMillis: Long = 0

    fun reset() {
        lastRefreshTimeMillis = timeProvider.millis()
    }

    override fun onRoutesRefreshed(result: RouteRefresherResult) {
        val currentTime = timeProvider.millis()
        if (result.success) {
            lastRefreshTimeMillis = currentTime
            observersManager.onRoutesRefreshed(result)
        } else {
            if (currentTime >= lastRefreshTimeMillis + staleDataTimeoutMillis) {
                lastRefreshTimeMillis = currentTime
                val newRoutes = expiringDataRemover.removeExpiringDataFromRoutes(
                    result.refreshedRoutes,
                    result.routeProgressData.legIndex
                )
                if (result.refreshedRoutes != newRoutes) {
                    val processedResult = RouteRefresherResult(
                        result.success,
                        newRoutes,
                        result.routeProgressData
                    )
                    observersManager.onRoutesRefreshed(processedResult)
                }
            }
        }
    }
}
