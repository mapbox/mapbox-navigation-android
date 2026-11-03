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

    // The last refresh that brought more than the charge.
    private var lastRefreshTimeMillis: Long = 0

    // The expiring data was removed, and no refresh has brought new traffic since. The device
    // keeps refreshing the charge, so the routes it refreshes are cleared too.
    private var expiringDataRemoved = false

    fun reset() {
        lastRefreshTimeMillis = timeProvider.millis()
        expiringDataRemoved = false
    }

    override fun onRoutesRefreshed(result: RoutesRefresherResult) {
        val currentTime = timeProvider.millis()
        // One timer for all the routes: they are refreshed together, from the same network state.
        if (result.anyFullSuccess()) {
            lastRefreshTimeMillis = currentTime
            expiringDataRemoved = false
            observersManager.onRoutesRefreshed(result)
            return
        }
        val staleDataTimedOut = currentTime >= lastRefreshTimeMillis + staleDataTimeoutMillis
        if (staleDataTimedOut) {
            lastRefreshTimeMillis = currentTime
            expiringDataRemoved = true
        }
        if (result.anySuccess()) {
            // Only the charge was refreshed. A route backed by a native route object can't be
            // cleared, and keeps its isUpToDate.
            val newRoutesResult = if (expiringDataRemoved) {
                expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
            } else {
                result
            }
            if (staleDataTimedOut) {
                stateHolder.onClearedExpired()
            }
            observersManager.onRoutesRefreshed(newRoutesResult)
        } else if (staleDataTimedOut) {
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
