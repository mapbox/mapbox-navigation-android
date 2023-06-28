package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.core.RoutesInvalidatedObserver
import com.mapbox.navigation.core.RoutesInvalidatedParams
import java.util.concurrent.CopyOnWriteArraySet

internal fun interface RouteRefreshObserver {

    fun onRoutesRefreshed(routeInfo: RoutesRefresherResult)
}

internal class RefreshObserversManager {

    private val refreshObservers = CopyOnWriteArraySet<RouteRefreshObserver>()
    private val invalidatedObservers = CopyOnWriteArraySet<RoutesInvalidatedObserver>()

    fun registerRefreshObserver(observer: RouteRefreshObserver) {
        refreshObservers.add(observer)
    }

    fun unregisterRefreshObserver(observer: RouteRefreshObserver) {
        refreshObservers.remove(observer)
    }

    fun registerInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        invalidatedObservers.add(observer)
    }

    fun unregisterInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        invalidatedObservers.remove(observer)
    }

    fun unregisterAllObservers() {
        refreshObservers.clear()
        invalidatedObservers.clear()
    }

    fun onRoutesRefreshed(result: RoutesRefresherResult) {
        refreshObservers.forEach { observer ->
            observer.onRoutesRefreshed(result)
        }
    }

    fun onRoutesInvalidated(params: RoutesInvalidatedParams) {
        invalidatedObservers.forEach { observer ->
            observer.onRoutesInvalidated(params)
        }
    }
}
