package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface RouteRefresherProgressCallback {

    fun onStarted()

    fun onResult(routeRefresherResult: RouteRefresherResult)
}

internal class RouteRefresherExecutor(
    private val routeRefresher: RouteRefresher,
    private val scope: CoroutineScope,
    private val timeout: Long,
) {

    private var hasCurrentRequest = false

    fun postRoutesToRefresh(
        routes: List<NavigationRoute>,
        callback: RouteRefresherProgressCallback
    ) {
        scope.launch {
            if (!hasCurrentRequest) {
                hasCurrentRequest = true
                callback.onStarted()
                val result = routeRefresher.refresh(routes, timeout)
                callback.onResult(result)
                hasCurrentRequest = false
            }
        }
    }
}
