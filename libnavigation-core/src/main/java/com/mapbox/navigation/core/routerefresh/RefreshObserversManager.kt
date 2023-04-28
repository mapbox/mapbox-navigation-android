package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.core.RoutesRefreshData
import java.util.concurrent.CopyOnWriteArraySet

internal fun interface RouteRefreshObserver {

    fun onRoutesRefreshed(routeInfo: RoutesRefreshData)
}

internal class RefreshObserversManager {

    private val refreshObservers = CopyOnWriteArraySet<RouteRefreshObserver>()

    fun registerObserver(observer: RouteRefreshObserver) {
        refreshObservers.add(observer)
    }

    fun unregisterObserver(observer: RouteRefreshObserver) {
        refreshObservers.remove(observer)
    }

    fun unregisterAllObservers() {
        refreshObservers.clear()
    }

    fun onRoutesRefreshed(result: RouteRefresherResult) {
        refreshObservers.forEach { observer ->
            observer.onRoutesRefreshed(result.refreshedRoutesData)
        }
    }
}
