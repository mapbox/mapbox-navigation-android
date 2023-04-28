package com.mapbox.navigation.navigator.internal.router

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.navigator.internal.mapToDirectionsResponse
import com.mapbox.navigator.GetRouteOptions
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouterDataRefCallback
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterRefreshCallback
import java.net.URL

private typealias NativeRouterCallback = com.mapbox.navigator.RouterCallback
private typealias NativeRouterRefreshCallback = com.mapbox.navigator.RouterRefreshCallback
private typealias NativeRouterOrigin = com.mapbox.navigator.RouterOrigin

class RouterInterfaceAdapter(
    private val router: NavigationRouter,
    private val getCurrentRoutesFun: () -> List<NavigationRoute>,
) : RouterInterface {

    @VisibleForTesting
    internal companion object {

        private const val ROUTE_REQUEST_FAILED_DEFAULT_MESSAGE = "Route request failed"
        private const val ROUTE_REQUEST_FAILED_EMPTY_ROUTES_LIST =
            "Route request failed. Empty routes list"
        private const val ROUTE_REQUEST_FAILED_DEFAULT_CODE = -1

        private const val ROUTE_REFRESH_FAILED_DEFAULT_MESSAGE = "Route refresh failed"

        @VisibleForTesting
        internal const val ROUTE_REFRESH_FAILED_DEFAULT_CODE = -1

        // take the latest item
        private fun List<RouterFailure>.mapToNativeRouteError(requestId: Long): RouterError =
            this.lastOrNull().let { routerFailure ->
                RouterError(
                    routerFailure?.message ?: ROUTE_REQUEST_FAILED_DEFAULT_MESSAGE,
                    routerFailure?.code ?: ROUTE_REQUEST_FAILED_DEFAULT_CODE,
                    RouterErrorType.UNKNOWN,
                    requestId,
                )
            }

        // take the latest item
        private fun List<RouterFailure>.fetchAndMapRouterOrigin(): NativeRouterOrigin =
            this.lastOrNull().let { routerFailure ->
                routerFailure?.routerOrigin?.mapToNativeRouteOrigin()
            } ?: NativeRouterOrigin.CUSTOM
    }

    override fun getRoute(
        directionsUri: String,
        options: GetRouteOptions, // unused, falling back default timeout for now
        callback: NativeRouterCallback
    ): Long {
        var requestId = -1L
        requestId =
            router.getRoute(
                RouteOptions.fromUrl(URL(directionsUri)),
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        val expected: Expected<RouterError, String> = if (routes.isNotEmpty()) {
                            ExpectedFactory.createValue(routes.mapToDirectionsResponse().toJson())
                        } else {
                            ExpectedFactory.createError(
                                RouterError(
                                    ROUTE_REQUEST_FAILED_EMPTY_ROUTES_LIST,
                                    ROUTE_REQUEST_FAILED_DEFAULT_CODE,
                                    RouterErrorType.UNKNOWN,
                                    requestId
                                )
                            )
                        }
                        callback.run(
                            expected,
                            routerOrigin.mapToNativeRouteOrigin(),
                        )
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        callback.run(
                            ExpectedFactory.createError(
                                reasons.mapToNativeRouteError(requestId)
                            ),
                            reasons.fetchAndMapRouterOrigin()
                        )
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        callback.run(
                            ExpectedFactory.createError(
                                RouterError(
                                    "Route request is canceled",
                                    ROUTE_REQUEST_FAILED_DEFAULT_CODE,
                                    RouterErrorType.REQUEST_CANCELLED,
                                    requestId,
                                )
                            ),
                            routerOrigin.mapToNativeRouteOrigin()
                        )
                    }
                }
            )
        return requestId
    }

    override fun getRoute(
        directionsUri: String,
        options: GetRouteOptions,
        callbackDataRef: RouterDataRefCallback
    ): Long {
        TODO("Not yet implemented")
    }

    override fun getRouteRefresh(
        options: RouteRefreshOptions,
        callback: RouterRefreshCallback
    ): Long {
        val route = getCurrentRoutesFun().find {
            it.directionsResponse.uuid() == options.requestId &&
                it.routeIndex == options.routeIndex
        }
        if (route != null) {
            return getRouteRefresh(options, route, callback)
        } else {
            callback.run(
                ExpectedFactory.createError(
                    RouterError(
                        "There is no route that matches $options, nothing to refresh.",
                        ROUTE_REFRESH_FAILED_DEFAULT_CODE,
                        RouterErrorType.UNKNOWN,
                        -1
                    )

                ),
                com.mapbox.navigator.RouterOrigin.CUSTOM,
                hashMapOf(),
            )
            return -1
        }
    }

    private fun getRouteRefresh(
        options: RouteRefreshOptions,
        route: NavigationRoute,
        callback: NativeRouterRefreshCallback
    ): Long {
        var requestId = -1L
        requestId = router.getRouteRefresh(
            route,
            options.legIndex,
            object : NavigationRouterRefreshCallback {
                override fun onRefreshReady(route: NavigationRoute) {
                    callback.run(
                        ExpectedFactory.createValue(route.directionsResponse.toJson()),
                        NativeRouterOrigin.CUSTOM,
                        hashMapOf()
                    )
                }

                override fun onFailure(error: NavigationRouterRefreshError) {
                    callback.run(
                        ExpectedFactory.createError(
                            RouterError(
                                error.message
                                    ?: error.throwable?.message
                                    ?: ROUTE_REFRESH_FAILED_DEFAULT_MESSAGE,
                                ROUTE_REFRESH_FAILED_DEFAULT_CODE,
                                RouterErrorType.UNKNOWN,
                                requestId,
                            )
                        ),
                        NativeRouterOrigin.CUSTOM,
                        hashMapOf()
                    )
                }
            }
        )
        return requestId
    }

    override fun cancelRouteRequest(token: Long) {
        router.cancelRouteRequest(token)
    }

    override fun cancelRouteRefreshRequest(token: Long) {
        router.cancelRouteRefreshRequest(token)
    }

    override fun cancelAll() {
        router.cancelAll()
    }
}
