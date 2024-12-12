/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.internal.router

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.MapboxServices
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.route.refreshRoute
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.route.updateExpirationTime
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.internal.utils.Constants.RouteResponse.KEY_REFRESH_TTL
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.internal.utils.RouteParsingManager
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.internal.utils.isErrorRetryable
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigation.base.route.RouterFailureType.Companion.RESPONSE_PARSING_ERROR
import com.mapbox.navigation.core.internal.performance.RouteParsingTracking
import com.mapbox.navigation.navigator.internal.mapToRoutingMode
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.GetRouteOptions
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RoutingProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

internal class RouterWrapper(
    router: RouterInterface,
    private val threadController: ThreadController,
    private val routeParsingManager: RouteParsingManager,
    private val routeParsingTracking: RouteParsingTracking,
) : Router {

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    private val routerRef = AtomicReference(router)

    @VisibleForTesting
    internal val router: RouterInterface
        get() = routerRef.get()

    fun resetRouter(router: RouterInterface) {
        val oldRouter = routerRef.getAndSet(router)
        oldRouter.cancelAll()
    }

    override fun getRoute(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        callback: NavigationRouterCallback,
    ): Long {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val routeUrl = routeOptions.toUrl(accessToken).toString()
        val requestOptions = GetRouteOptions(null) // using default timeout (5 seconds)

        val urlWithoutToken = URL(routeUrl.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM))
        logD(LOG_CATEGORY) { "requesting route for $urlWithoutToken" }
        val originRouter = router
        return originRouter.getRoute(
            routeUrl,
            requestOptions,
            signature.toNativeSignature(),
        ) { result, origin ->
            logD(LOG_CATEGORY) {
                "received result from router.getRoute for $urlWithoutToken; origin: $origin"
            }

            mainJobControl.scope.launch {
                if (originRouter != router) {
                    logD(LOG_CATEGORY) { "router was recreated, onFailure callback will be fired" }
                    callback.onFailure(
                        listOf(
                            RouterFailureFactory.create(
                                url = urlWithoutToken,
                                routerOrigin = origin.mapToSdkRouteOrigin(),
                                message = "Failed to get a route",
                                type = RouterFailureType.UNKNOWN_ERROR,
                            ),
                        ),
                        routeOptions,
                    )
                } else {
                    result.fold(
                        { errors ->
                            // https://mapbox.atlassian.net/browse/NN-1733
                            // NN should guarantee that there's at most one cancelled request
                            if (errors.any { it.type == RouterErrorType.REQUEST_CANCELLED }) {
                                logI(
                                    "Route request cancelled: $routeOptions, $origin",
                                    LOG_CATEGORY,
                                )
                                callback.onCanceled(routeOptions, origin.mapToSdkRouteOrigin())
                            } else {
                                val failures = errors.map {
                                    RouterFailureFactory.create(
                                        url = urlWithoutToken,
                                        routerOrigin = origin.mapToSdkRouteOrigin(),
                                        message = it.message,
                                        type = it.type.mapToSdkRouterFailureType(),
                                        throwable = null,
                                        isRetryable = it.isErrorRetryable,
                                    )
                                }

                                logW("Route request failed with: $failures", LOG_CATEGORY)
                                callback.onFailure(failures, routeOptions)
                            }
                        },
                        { responseBody ->
                            mainJobControl.scope.launch {
                                logI(
                                    "processing successful response " +
                                        "from router.getRoute for $urlWithoutToken",
                                    LOG_CATEGORY,
                                )
                                val responseInfo =
                                    RouteResponseInfo.fromResponse(responseBody.buffer)
                                routeParsingManager.parseRouteResponse(responseInfo) {
                                    val responseTimeElapsedMillis = Time.SystemClockImpl.millis()
                                    parseDirectionsResponse(
                                        ThreadController.DefaultDispatcher,
                                        responseBody,
                                        routeUrl,
                                        origin.mapToSdkRouteOrigin(),
                                        responseTimeElapsedMillis,
                                    ).fold(
                                        { throwable ->
                                            callback.onFailure(
                                                listOf(
                                                    RouterFailureFactory.create(
                                                        url = urlWithoutToken,
                                                        routerOrigin = origin.mapToSdkRouteOrigin(),
                                                        message = "Failed to parse response",
                                                        type = RESPONSE_PARSING_ERROR,
                                                        throwable = throwable,
                                                    ),
                                                ),
                                                routeOptions,
                                            )
                                        },
                                        { response ->
                                            val routes = response.routes
                                            val routeOrigin = origin.mapToSdkRouteOrigin()

                                            logI(
                                                "Routes parsing completed: ${routes.map { it.id }}",
                                                LOG_CATEGORY,
                                            )

                                            routeParsingTracking.routeResponseIsParsed(
                                                response.meta,
                                            )
                                            callback.onRoutesReady(routes, routeOrigin)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    override fun getRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback,
    ): Long {
        val routeOptions = route.routeOptions
        val requestUuid = route.directionsRoute.requestUuid()
        val routeIndex = route.routeIndex
        if (requestUuid.isNullOrBlank()) {
            val errorMessage =
                """
                   Route refresh failed because of a empty or null param:
                   requestUuid = $requestUuid
                """.trimIndent()

            logW(errorMessage, LOG_CATEGORY)

            callback.onFailure(
                NavigationRouterRefreshError(
                    "Route refresh failed",
                    Exception(errorMessage),
                ),
            )

            return REQUEST_FAILURE
        }

        val refreshOptions = RouteRefreshOptions(
            requestUuid,
            routeIndex,
            routeRefreshRequestData.legIndex,
            RoutingProfile(routeOptions.profile().mapToRoutingMode(), routeOptions.user()),
            routeOptions.baseUrl(),
            routeRefreshRequestData.routeGeometryIndex,
            HashMap(routeRefreshRequestData.experimentalProperties),
        )

        val originRouter = router
        return originRouter.getRouteRefresh(
            refreshOptions,
        ) { result, _, _ ->
            logI("Received result from router.getRouteRefresh for ${route.id}", LOG_CATEGORY)

            mainJobControl.scope.launch {
                if (originRouter != router) {
                    logD(LOG_CATEGORY) { "router was recreated, onFailure callback will be fired" }
                    callback.onFailure(
                        NavigationRouterRefreshError("Failed to refresh a route"),
                    )
                } else {
                    val responseTimeElapsedSeconds = Time.SystemClockImpl.seconds()
                    result.fold(
                        { errors ->
                            val error = errors.first()

                            val errorMessage =
                                """
                               Route refresh failed.
                               requestUuid = $requestUuid
                               message = ${error.message}
                               type = ${error.type}
                               requestId = ${error.requestId}
                               refreshTTL = ${error.refreshTtl}
                               routeRefreshRequestData = $routeRefreshRequestData
                                """.trimIndent()

                            logW(errorMessage, LOG_CATEGORY)

                            error.refreshTtl?.let {
                                route.updateExpirationTime(it + responseTimeElapsedSeconds)
                            }
                            callback.onFailure(
                                NavigationRouterRefreshError(
                                    "Route refresh failed",
                                    Exception(errorMessage),
                                    refreshTtl = error.refreshTtl,
                                ),
                            )
                        },
                        {
                            mainJobControl.scope.launch {
                                withContext(ThreadController.DefaultDispatcher) {
                                    parseDirectionsRouteRefresh(it)
                                        .onValue {
                                            logD(
                                                "Parsed route refresh response for " +
                                                    "route(${route.id})",
                                                LOG_CATEGORY,
                                            )
                                        }
                                        .onError {
                                            logD(
                                                "Failed to parse route refresh response for " +
                                                    "route(${route.id})",
                                                LOG_CATEGORY,
                                            )
                                        }
                                        .mapValue { routeRefresh ->
                                            val updatedWaypoints = WaypointsParser.parse(
                                                routeRefresh.unrecognizedJsonProperties
                                                    ?.get(Constants.RouteResponse.KEY_WAYPOINTS),
                                            )
                                            route.refreshRoute(
                                                refreshOptions.legIndex,
                                                routeRefreshRequestData.legGeometryIndex,
                                                routeRefresh.legs()?.map { it.annotation() },
                                                routeRefresh.legs()?.map { it.incidents() },
                                                routeRefresh.legs()?.map { it.closures() },
                                                updatedWaypoints,
                                                responseTimeElapsedSeconds,
                                                routeRefresh.unrecognizedJsonProperties
                                                    ?.get(KEY_REFRESH_TTL)?.asInt,
                                            )
                                        }
                                }.fold(
                                    { throwable ->
                                        callback.onFailure(
                                            NavigationRouterRefreshError(
                                                "failed for response: $it",
                                                throwable,
                                            ),
                                        )
                                    },
                                    {
                                        callback.onRefreshReady(it)
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
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
