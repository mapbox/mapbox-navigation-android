/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.route.internal

import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.InternalRouter
import com.mapbox.navigation.base.internal.route.refreshRoute
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.navigator.internal.mapToRoutingMode
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
import com.mapbox.navigation.route.internal.util.parseDirectionsRouteRefresh
import com.mapbox.navigation.route.internal.util.redactQueryParam
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RoutingProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@MapboxModule(MapboxModuleType.NavigationRouter)
class RouterWrapper(
    private val accessToken: String,
    private val router: RouterInterface,
    private val threadController: ThreadController,
) : NavigationRouter, InternalRouter {

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    override fun getRoute(routeOptions: RouteOptions, callback: NavigationRouterCallback): Long {
        val routeUrl = routeOptions.toUrl(accessToken).toString()

        return router.getRoute(routeUrl) { result, origin ->
            val urlWithoutToken = URL(routeUrl.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM))
            logI("received result from route.getRoute for $urlWithoutToken", LOG_CATEGORY)
            result.fold(
                {
                    mainJobControl.scope.launch {
                        if (it.type == RouterErrorType.REQUEST_CANCELLED) {
                            logI(
                                """
                                    Route request cancelled:
                                    $routeOptions
                                    $origin
                                """.trimIndent(),
                                LOG_CATEGORY
                            )
                            callback.onCanceled(routeOptions, origin.mapToSdkRouteOrigin())
                        } else {
                            val failureReasons = listOf(
                                RouterFailure(
                                    url = urlWithoutToken,
                                    routerOrigin = origin.mapToSdkRouteOrigin(),
                                    message = it.message,
                                    code = it.code
                                )
                            )

                            logW(
                                """
                                    Route request failed with:
                                    $failureReasons
                                """.trimIndent(),
                                LOG_CATEGORY
                            )

                            callback.onFailure(failureReasons, routeOptions)
                        }
                    }
                },
                {
                    mainJobControl.scope.launch {
                        logI(
                            "processing successful response " +
                                "from router.getRoute for $urlWithoutToken",
                            LOG_CATEGORY
                        )
                        parseDirectionsResponse(
                            ThreadController.DefaultDispatcher,
                            it,
                            routeUrl,
                            origin.mapToSdkRouteOrigin(),
                        ).fold(
                            { throwable ->
                                callback.onFailure(
                                    listOf(
                                        RouterFailure(
                                            urlWithoutToken,
                                            origin.mapToSdkRouteOrigin(),
                                            "failed for response: $it",
                                            throwable = throwable
                                        )
                                    ),
                                    routeOptions
                                )
                            },
                            { routes ->
                                val metadata = routes.firstOrNull()?.directionsResponse?.metadata()
                                logI("Response metadata: $metadata", LOG_CATEGORY)
                                callback.onRoutesReady(
                                    routes,
                                    origin.mapToSdkRouteOrigin()
                                )
                            }
                        )
                    }
                }
            )
        }
    }

    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback): Long {
        return getRoute(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    callback.onRoutesReady(routes.toDirectionsRoutes(), routerOrigin)
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
        val routeOptions = route.routeOptions
        val requestUuid = route.directionsResponse.uuid()
        val routeIndex = route.routeIndex
        if (requestUuid == null || requestUuid.isBlank()) {
            val errorMessage =
                """
                   Route refresh failed because of a empty or null param:
                   requestUuid = $requestUuid
                """.trimIndent()

            logW(errorMessage, LOG_CATEGORY)

            callback.onFailure(
                RouterFactory.buildNavigationRouterRefreshError(
                    "Route refresh failed",
                    Exception(errorMessage)
                )
            )

            return REQUEST_FAILURE
        }

        val refreshOptions = RouteRefreshOptions(
            requestUuid,
            routeIndex,
            legIndex,
            RoutingProfile(routeOptions.profile().mapToRoutingMode(), routeOptions.user())
        )

        return router.getRouteRefresh(
            refreshOptions
        ) { result, _ ->
            logI("Received result from router.getRouteRefresh for ${route.id}", LOG_CATEGORY)
            result.fold(
                {
                    mainJobControl.scope.launch {
                        val errorMessage =
                            """
                               Route refresh failed.
                               message = ${it.message}
                               code = ${it.code}
                               type = ${it.type}
                               requestId = ${it.requestId}
                               legIndex = $legIndex
                            """.trimIndent()

                        logW(errorMessage, LOG_CATEGORY)

                        callback.onFailure(
                            RouterFactory.buildNavigationRouterRefreshError(
                                "Route refresh failed", Exception(errorMessage)
                            )
                        )
                    }
                },
                {
                    mainJobControl.scope.launch {
                        withContext(ThreadController.DefaultDispatcher) {
                            parseDirectionsRouteRefresh(it)
                                .onValue {
                                    logI(
                                        "Parsed route refresh response for route(${route.id})",
                                        LOG_CATEGORY
                                    )
                                }
                                .onError {
                                    logI(
                                        "Failed to parse route refresh response for " +
                                            "route(${route.id})",
                                        LOG_CATEGORY
                                    )
                                }
                                .mapValue { routeRefresh ->
                                    route.refreshRoute(
                                        initialLegIndex = refreshOptions.legIndex,
                                        legAnnotations = routeRefresh.legs()?.map {
                                            it.annotation()
                                        },
                                        incidents = routeRefresh.legs()?.map {
                                            it.incidents()
                                        }
                                    )
                                }
                        }.fold(
                            { throwable ->
                                callback.onFailure(
                                    RouterFactory.buildNavigationRouterRefreshError(
                                        "failed for response: $it", throwable,
                                    )
                                )
                            },
                            { callback.onRefreshReady(it) },
                        )
                    }
                }
            )
        }
    }

    override fun getRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long {
        return getRouteRefresh(
            route.toNavigationRoute(),
            legIndex,
            object : NavigationRouterRefreshCallback {
                override fun onRefreshReady(route: NavigationRoute) {
                    callback.onRefresh(route.directionsRoute)
                }

                override fun onFailure(error: NavigationRouterRefreshError) {
                    callback.onError(
                        RouteRefreshError(error.message, error.throwable)
                    )
                }
            }
        )
    }

    override fun cancelRouteRequest(requestId: Long) {
        router.cancelRouteRequest(requestId)
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
        router.cancelRouteRefreshRequest(requestId)
    }

    override fun cancelAll() {
        router.cancelAll()
    }

    override fun shutdown() {
        router.cancelAll()
    }

    private companion object {
        private const val LOG_CATEGORY = "RouterWrapper"
        private const val REQUEST_FAILURE = -1L
    }
}
