package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute

internal class RouteRefresherExecutor(
    private val routeRefresher: RouteRefresher,
    private val timeout: Long,
) {

    private var hasCurrentRequest = false

    suspend fun executeRoutesRefresh(
        routes: List<NavigationRoute>,
        startCallback: () -> Unit
    ): Expected<String, RouteRefresherResult> {
        if (!hasCurrentRequest) {
            hasCurrentRequest = true
            startCallback()
            try {
                return ExpectedFactory.createValue(routeRefresher.refresh(routes, timeout))
            } finally {
                hasCurrentRequest = false
            }
        } else {
            return ExpectedFactory.createError("Skipping request as another one is in progress.")
        }
    }
}
