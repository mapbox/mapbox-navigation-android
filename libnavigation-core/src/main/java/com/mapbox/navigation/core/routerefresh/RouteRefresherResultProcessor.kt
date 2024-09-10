package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.Time

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefresherResultProcessor(
    private val stateHolder: RouteRefreshStateHolder,
    private val observersManager: RefreshObserversManager,
    private val expiringDataRemover: ExpiringDataRemover,
    private val timeProvider: Time,
    private val staleDataTimeoutMillis: Long,
) : RouteRefresherListener {

    private var lastRefreshTimeMillis: Long = 0

    fun reset() {
        lastRefreshTimeMillis = timeProvider.millis()
    }

    override fun onRoutesRefreshed(result: RoutesRefresherResult) {
        val currentTime = timeProvider.millis()
        if (result.anySuccess()) {
            lastRefreshTimeMillis = currentTime
            observersManager.onRoutesRefreshed(result)
        } else {
            if (currentTime >= lastRefreshTimeMillis + staleDataTimeoutMillis) {
                lastRefreshTimeMillis = currentTime
                val newRoutesResult = expiringDataRemover.removeExpiringDataFromRoutesProgressData(
                    result,
                )
                stateHolder.onClearedExpired()
                if (result != newRoutesResult) {
                    observersManager.onRoutesRefreshed(newRoutesResult)
                }
            }
        }
    }
}
