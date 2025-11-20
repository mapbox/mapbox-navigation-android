/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.internal.router

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.common.MapboxServices
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.internalRefreshRoute
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.route.updateExpirationTime
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.internal.utils.RouteParsingManager
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.internal.utils.isErrorRetryable
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
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
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.RoutingProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.ExperimentalTime

private class OngoingRequest(
    var parsingJob: Job?,
    val onCancel: () -> Unit,
)

@OptIn(ExperimentalTime::class)
@MainThread // Router is not thread safe: in theory it doesn't have to be main thread
internal class RouterWrapper(
    router: RouterInterface,
    private val threadController: ThreadController,
    private val routeParsingManager: RouteParsingManager,
    private val routeParsingTracking: RouteParsingTracking,
) : Router {

    private val activeRouteRequests = mutableMapOf<Long, OngoingRequest>()
    private val activeRouteRefreshRequests = mutableMapOf<Long, OngoingRequest>()

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
        logI(LOG_CATEGORY) { "requesting route for $urlWithoutToken" }
        val originRouter = router
        var callbackInvoked = false
        var id: Long? = null

        val getRouteSection = PerformanceTracker.asyncSectionStarted("RouterWrapper#getRoute()")

        id = originRouter.getRoute(
            routeUrl,
            requestOptions,
            signature.toNativeSignature(),
        ) { result, origin ->
            callbackInvoked = true
            logD(LOG_CATEGORY) {
                "received result from router.getRoute for $urlWithoutToken; origin: $origin"
            }

            mainJobControl.scope.launch {
                PerformanceTracker.asyncSectionCompleted(getRouteSection)

                endRouteRequest(
                    id,
                    routeOptions,
                    routeUrl,
                    urlWithoutToken,
                    originRouter,
                    result,
                    origin,
                    RouteRequestEnder(id, callback),
                )
            }
        }
        if (!callbackInvoked) {
            activeRouteRequests[id] = OngoingRequest(
                null,
                {
                    PerformanceTracker.asyncSectionCompleted(getRouteSection)

                    callback.onCanceled(
                        routeOptions,
                        com.mapbox.navigation.base.route.RouterOrigin.OFFLINE,
                    )
                },
            )
        }
        return id
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
        var id: Long? = null
        var callbackInvoked = false

        val routeRefreshSection = PerformanceTracker.asyncSectionStarted(
            "RouterWrapper#getRouteRefresh()",
        )

        id = originRouter.getRouteRefresh(
            refreshOptions,
        ) { result, _, _ ->
            callbackInvoked = true
            logI("Received result from router.getRouteRefresh for ${route.id}", LOG_CATEGORY)

            mainJobControl.scope.launch {
                PerformanceTracker.asyncSectionCompleted(routeRefreshSection)

                endRouteRefreshRequest(
                    id,
                    originRouter,
                    requestUuid,
                    routeRefreshRequestData,
                    refreshOptions,
                    route,
                    result,
                    RouteRefreshRequestEnder(id, callback),
                )
            }
        }
        if (!callbackInvoked) {
            activeRouteRefreshRequests[id] = OngoingRequest(
                null,
                {
                    PerformanceTracker.asyncSectionCompleted(routeRefreshSection)

                    callback.onFailure(NavigationRouterRefreshError("Request cancelled"))
                },
            )
        }
        return id
    }

    override fun cancelRouteRequest(requestId: Long) {
        activeRouteRequests[requestId]?.let {
            it.onCancel()
            activeRouteRequests.remove(requestId)
            it.parsingJob?.cancel()
        }
        router.cancelRouteRequest(requestId)
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
        activeRouteRefreshRequests[requestId]?.let {
            it.onCancel()
            activeRouteRefreshRequests.remove(requestId)
            it.parsingJob?.cancel()
        }
        router.cancelRouteRefreshRequest(requestId)
    }

    override fun cancelAll() {
        activeRouteRequests.toList().forEach { (id, request) ->
            request.onCancel()
            activeRouteRequests.remove(id)
            request.parsingJob?.cancel()
        }
        activeRouteRefreshRequests.toList().forEach { (id, request) ->
            request.onCancel()
            activeRouteRefreshRequests.remove(id)
            request.parsingJob?.cancel()
        }
        router.cancelAll()
    }

    override fun shutdown() {
        cancelAll()
    }

    private fun endRouteRequest(
        id: Long?,
        routeOptions: RouteOptions,
        routeUrl: String,
        urlWithoutToken: URL,
        originRouter: RouterInterface,
        result: Expected<List<RouterError>, DataRef>,
        origin: RouterOrigin,
        requestEnder: RouteRequestEnder,
    ) {
        if (id != null && activeRouteRequests[id] == null) {
            logI(LOG_CATEGORY) { "Response for request $id has already been processed" }
            return
        }

        if (originRouter != router) {
            logD(LOG_CATEGORY) { "router was recreated, onFailure callback will be fired" }
            requestEnder.onFailure(
                listOf(
                    RouterFailureFactory.create(
                        url = urlWithoutToken,
                        routerOrigin = origin.mapToSdkRouteOrigin(),
                        message = "Failed to get a route",
                        type = RouterFailureType.ROUTER_RECREATION_ERROR,
                        isRetryable = true,
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
                        requestEnder.onCanceled(
                            routeOptions,
                            origin.mapToSdkRouteOrigin(),
                        )
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
                        requestEnder.onFailure(failures, routeOptions)
                    }
                },
                { responseBody ->
                    mainJobControl.scope.launch {
                        logI(
                            "processing successful response " +
                                "from router.getRoute for $urlWithoutToken",
                            LOG_CATEGORY,
                        )

                        if (id != null) {
                            if (activeRouteRequests[id] == null) {
                                logI { "Request $id has been processed before" }
                                return@launch
                            } else {
                                activeRouteRequests[id]?.parsingJob =
                                    this@launch.coroutineContext[Job]
                            }
                        }

                        val responseInfo =
                            RouteResponseInfo.fromResponse(responseBody.buffer)
                        routeParsingManager.parseRouteResponse(responseInfo) {
                            val responseTimeElapsedMillis =
                                Time.SystemClockImpl.millis()
                            val parsingResult = parseDirectionsResponse(
                                ThreadController.DefaultDispatcher,
                                responseBody,
                                routeUrl,
                                origin.mapToSdkRouteOrigin(),
                                responseTimeElapsedMillis,
                            )
                            parsingResult.fold(
                                { throwable ->
                                    requestEnder.onFailure(
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
                                    val routeOrigin =
                                        origin.mapToSdkRouteOrigin()

                                    logI(
                                        "Routes parsing completed: ${routes.map { it.id }}",
                                        LOG_CATEGORY,
                                    )

                                    routeParsingTracking.routeResponseIsParsed(
                                        response.meta,
                                    )
                                    requestEnder.onRoutesReady(routes, routeOrigin)
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    private fun endRouteRefreshRequest(
        id: Long?,
        originRouter: RouterInterface,
        requestUuid: String,
        routeRefreshRequestData: RouteRefreshRequestData,
        refreshOptions: RouteRefreshOptions,
        route: NavigationRoute,
        result: Expected<List<RouterError>, DataRef>,
        callback: RouteRefreshRequestEnder,
    ) {
        if (id != null && activeRouteRefreshRequests[id] == null) {
            logI { "Response for refresh request $id has already been processed" }
            return
        }
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
                        if (id != null) {
                            if (activeRouteRefreshRequests[id] == null) {
                                logI { "Refresh request $id has already been processed" }
                                return@launch
                            } else {
                                activeRouteRefreshRequests[id]?.parsingJob =
                                    this@launch.coroutineContext[Job]
                            }
                        }
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
                                    route.internalRefreshRoute(
                                        routeRefresh,
                                        refreshOptions.legIndex,
                                        routeRefreshRequestData.legGeometryIndex,
                                        responseTimeElapsedSeconds,
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

    private inner class RouteRequestEnder(
        private val id: Long?,
        private val callback: NavigationRouterCallback,
    ) : NavigationRouterCallback {

        override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
            if (removeRequest()) {
                callback.onRoutesReady(routes, routerOrigin)
            }
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            if (removeRequest()) {
                callback.onFailure(reasons, routeOptions)
            }
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
            if (removeRequest()) {
                callback.onCanceled(routeOptions, routerOrigin)
            }
        }

        private fun removeRequest(): Boolean {
            return if (id == null) {
                true
            } else {
                activeRouteRequests.remove(id) != null
            }
        }
    }

    private inner class RouteRefreshRequestEnder(
        private val id: Long?,
        private val callback: NavigationRouterRefreshCallback,
    ) : NavigationRouterRefreshCallback {

        override fun onRefreshReady(route: NavigationRoute) {
            if (removeRequest()) {
                callback.onRefreshReady(route)
            }
        }

        override fun onFailure(error: NavigationRouterRefreshError) {
            if (removeRequest()) {
                callback.onFailure(error)
            }
        }

        private fun removeRequest(): Boolean {
            return if (id == null) {
                true
            } else {
                activeRouteRefreshRequests.remove(id) != null
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "RouterWrapper"
        private const val REQUEST_FAILURE = -1L
    }
}
