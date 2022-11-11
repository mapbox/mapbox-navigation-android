package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface RouteRefresherProgressCallback {

    fun onStarted()

    fun onResult(routeRefresherResult: RouteRefresherResult)
}

internal class RouteRefresherExecutor(
    private val routeRefresher: RouteRefresher,
    private val scope: CoroutineScope,
    private val timeout: Long,
) {

    private val mutex = Mutex()
    private val queue = ArrayDeque<Pair<List<NavigationRoute>, RouteRefresherProgressCallback>>()

    fun postRoutesToRefresh(
        routes: List<NavigationRoute>,
        callback: RouteRefresherProgressCallback
    ) {
        queue.clear()
        queue.add(routes to callback)
        scope.launch {
            mutex.withLock {
                queue.removeFirstOrNull()?.let {
                    it.second.onStarted()
                    val result = routeRefresher.refresh(it.first, timeout)
                    it.second.onResult(result)
                }
            }
        }
    }
}
