package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.v5.route.FasterRoute
import com.mapbox.services.android.navigation.v5.route.RouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import timber.log.Timber

internal class NavigationFasterRouteListener(
    private val navigationEventDispatcher: NavigationEventDispatcher,
    private val fasterRouteEngine: FasterRoute
) : RouteListener {

    companion object {
        private const val FIRST_ROUTE = 0
    }

    override fun onResponseReceived(response: DirectionsResponse, routeProgress: RouteProgress?) {
        ifNonNull(routeProgress) {
            if (fasterRouteEngine.isFasterRoute(response, it)) {
                navigationEventDispatcher.onFasterRouteEvent(response.routes()[FIRST_ROUTE])
            }
        }
    }

    override fun onErrorReceived(throwable: Throwable) {
        Timber.e(throwable)
    }
}
