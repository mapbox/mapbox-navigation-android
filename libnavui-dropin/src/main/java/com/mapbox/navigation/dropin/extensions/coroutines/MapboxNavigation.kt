package com.mapbox.navigation.dropin.extensions.coroutines

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RoutesWithOrigin(
    val routes: List<DirectionsRoute>,
    val routerOrigin: RouterOrigin
)

class RequestRoutesError(
    val reasons: List<RouterFailure>,
    message: String
) : Error(message)

suspend fun MapboxNavigation.requestRoutes(routeOptions: RouteOptions): RoutesWithOrigin =
    suspendCancellableCoroutine { continuation ->
        requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    continuation.resume(RoutesWithOrigin(routes, routerOrigin))
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    val error = RequestRoutesError(
                        reasons,
                        "Route request failed with: $reasons"
                    )
                    continuation.resumeWithException(error)
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    continuation.cancel()
                }
            }
        )
    }
