package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun MapboxNavigation.fetchRoute(
    routeOptions: RouteOptions,
    fetchStarted: (requestId: Long) -> Unit = {}
): List<NavigationRoute> {
    return suspendCancellableCoroutine { cont ->
        val requestId = requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    cont.resume(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    cont.resumeWithException(FetchRouteError(reasons, routeOptions))
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) {
                    cont.cancel(FetchRouteCancelled(routeOptions, routerOrigin))
                }
            }
        )
        fetchStarted(requestId)
        cont.invokeOnCancellation { cancelRouteRequest(requestId) }
    }
}

internal class FetchRouteError(
    val reasons: List<RouterFailure>,
    val routeOptions: RouteOptions
) : Error()

internal class FetchRouteCancelled(
    val routeOptions: RouteOptions,
    val routerOrigin: RouterOrigin
) : Error()
