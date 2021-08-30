/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.route.internal

import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
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
import org.json.JSONObject
import java.net.URL

@MapboxModule(MapboxModuleType.NavigationRouter)
class RouterWrapper(
    private val accessToken: String,
    private val router: RouterInterface,
    private val threadController: ThreadController,
) : Router {

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback): Long {
        val routeUrl = routeOptions.toUrl(accessToken).toString()

        return router.getRoute(routeUrl) { result, origin ->
            val urlWithoutToken = URL(routeUrl.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM))
            result.fold(
                {
                    mainJobControl.scope.launch {
                        if (it.type == RouterErrorType.REQUEST_CANCELLED) {
                            logI(
                                TAG,
                                Message(
                                    """
                                        Route request cancelled:
                                        $routeOptions
                                        $origin
                                    """.trimIndent()
                                )
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
                                TAG,
                                Message(
                                    """
                                        Route request failed with:
                                        $failureReasons
                                    """.trimIndent()
                                )
                            )

                            callback.onFailure(failureReasons, routeOptions)
                        }
                    }
                },
                {
                    mainJobControl.scope.launch {
                        val routes = parseDirectionsResponse(it, routeOptions)
                        if (routes.isNullOrEmpty()) {
                            callback.onFailure(
                                listOf(
                                    RouterFailure(
                                        urlWithoutToken,
                                        origin.mapToSdkRouteOrigin(),
                                        ROUTES_LIST_EMPTY
                                    )
                                ),
                                routeOptions
                            )
                        } else {
                            callback.onRoutesReady(routes, origin.mapToSdkRouteOrigin())
                        }
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
        val routeOptions = route.routeOptions()
        val requestUuid = route.requestUuid()
        val routeIndex = route.routeIndex()?.toIntOrNull()
        if (routeOptions == null || requestUuid == null || routeIndex == null) {
            val errorMessage =
                """
                   Route refresh failed because of a null param:
                   routeOptions = $routeOptions
                   requestUuid = $requestUuid
                   routeIndex = $routeIndex
                """.trimIndent()

            logW(TAG, Message(errorMessage))

            callback.onError(
                RouteRefreshError("Route refresh failed", Exception(errorMessage))
            )

            return REQUEST_FAILURE
        }

        val refreshOptions = RouteRefreshOptions(
            requestUuid,
            routeIndex,
            legIndex,
            RoutingProfile(routeOptions.profile().mapToRoutingMode(), routeOptions.user())
        )

        return router.getRouteRefresh(refreshOptions, route.toJson()) { result, _ ->
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

                        logW(TAG, Message(errorMessage))

                        callback.onError(
                            RouteRefreshError("Route refresh failed", Exception(errorMessage))
                        )
                    }
                },
                {
                    mainJobControl.scope.launch {
                        val refreshedRoute =
                            withContext(ThreadController.IODispatcher) {
                                DirectionsRoute.fromJson(
                                    it,
                                    routeOptions,
                                    route.requestUuid()
                                )
                            }
                        callback.onRefresh(refreshedRoute)
                    }
                }
            )
        }
    }

    override fun cancelRouteRequest(requestId: Long) {
        router.cancelRequest(requestId)
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
        router.cancelRequest(requestId)
    }

    override fun cancelAll() {
        router.cancelAll()
    }

    override fun shutdown() {
        router.cancelAll()
    }

    private suspend fun parseDirectionsResponse(
        json: String,
        options: RouteOptions?
    ): List<DirectionsRoute> =
        withContext(ThreadController.IODispatcher) {
            val jsonObject = JSONObject(json)
            val uuid: String? = if (jsonObject.has(UUID)) {
                jsonObject.getString(UUID)
            } else {
                null
            }

            // TODO remove after https://github.com/mapbox/navigation-sdks/issues/1229
            if (jsonObject.has(METADATA)) {
                logI(TAG, Message("Response metadata: ${jsonObject.getString(METADATA)}"))
            }

            // TODO simplify when https://github.com/mapbox/mapbox-java/issues/1292 is finished
            val response = DirectionsResponse.fromJson(json, options, uuid)

            response.routes()
        }

    private companion object {
        private val TAG = Tag("MbxRouterWrapper")
        private const val UUID = "uuid"
        private const val METADATA = "metadata"
        private const val ROUTES_LIST_EMPTY = "routes list is empty"
        private const val REQUEST_FAILURE = -1L
    }
}
