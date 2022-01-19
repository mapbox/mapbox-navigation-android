package com.mapbox.navigation.core.directions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoutes

internal class LegacyRouterAdapter(
    private val legacyRouter: Router
) : NavigationRouter, Router by legacyRouter {
    override fun getRoute(routeOptions: RouteOptions, callback: NavigationRouterCallback): Long {
        return legacyRouter.getRoute(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    callback.onRoutesReady(routes.toNavigationRoutes(), routerOrigin)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    callback.onFailure(reasons, routeOptions)
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    callback.onCanceled(routeOptions, routerOrigin)
                }
            }
        )
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun getRouteRefresh(
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
    ): Long {
        return legacyRouter.getRouteRefresh(
            route.directionsRoute,
            legIndex,
            object : RouteRefreshCallback {
                override fun onRefresh(directionsRoute: DirectionsRoute) {
                    callback.onRefreshReady(directionsRoute.toNavigationRoute())
                }

                override fun onError(error: RouteRefreshError) {
                    callback.onFailure(
                        RouterFactory.buildNavigationRouterRefreshError(
                            error.message,
                            error.throwable
                        )
                    )
                }
            }
        )
    }
}
