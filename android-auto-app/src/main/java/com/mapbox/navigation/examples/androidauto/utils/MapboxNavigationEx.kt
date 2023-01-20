package com.mapbox.navigation.examples.androidauto.utils

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun MapboxNavigation.fetchRoute(
    origin: Point,
    destination: Point,
): List<NavigationRoute> =
    fetchRoute(
        RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(navigationOptions.applicationContext)
            .layersList(listOf(getZLevel(), null))
            .coordinatesList(listOf(origin, destination))
            .alternatives(true)
            .build()
    )

internal suspend fun MapboxNavigation.fetchRoute(
    routeOptions: RouteOptions
): List<NavigationRoute> = suspendCancellableCoroutine { cont ->
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
            ) = Unit
        }
    )
    cont.invokeOnCancellation { cancelRouteRequest(requestId) }
}

internal class FetchRouteError(
    val reasons: List<RouterFailure>,
    val routeOptions: RouteOptions
) : Error()
