package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ImmediateRouteRefreshController(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val stateHolder: RouteRefreshStateHolder,
    private val scope: CoroutineScope,
    private val listener: RouteRefresherListener,
    private val attemptListener: RoutesRefreshAttemptListener,
) {

    @Throws(IllegalArgumentException::class)
    fun requestRoutesRefresh(
        routes: List<NavigationRoute>,
        callback: (Expected<String, RoutesRefresherResult>) -> Unit
    ) {
        if (routes.isEmpty()) {
            throw IllegalArgumentException("Routes to refresh should not be empty")
        }
        scope.launch {
            val result = routeRefresherExecutor.executeRoutesRefresh(
                routes,
                startCallback = { stateHolder.onStarted() }
            )
            callback(result)
            result.fold(
                { logW("Route refresh on-demand error: $it", RouteRefreshLog.LOG_CATEGORY) },
                {
                    attemptListener.onRoutesRefreshAttemptFinished(it)
                    if (it.anySuccess()) {
                        stateHolder.onSuccess()
                    } else {
                        stateHolder.onFailure(null)
                    }
                    listener.onRoutesRefreshed(it)
                }
            )
        }
    }
}
