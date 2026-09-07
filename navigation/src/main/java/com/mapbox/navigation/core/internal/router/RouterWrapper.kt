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
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.internalRefreshRoute
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRouteParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.route.updateExpirationTime
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.internal.utils.isErrorRetryable
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigation.base.route.RouterFailureType.Companion.RESPONSE_PARSING_ERROR
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingFailure
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.mapmatching.MapMatchingSuccessfulResult
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

private interface RouteRequestCallback<T> {
    fun onSuccess(result: T, routerOrigin: String)
    fun onFailure(failures: List<RouterFailure>, routeOptions: RouteOptions)
    fun onCanceled(routeOptions: RouteOptions, routerOrigin: String)
}

private class OngoingRequest(
    var parsingJob: Job?,
    val onCancel: () -> Unit,
)

@OptIn(ExperimentalTime::class)
@MainThread // Router is not thread safe: in theory it doesn't have to be main thread
internal class RouterWrapper(
    router: RouterInterface,
    private val threadController: ThreadController,
    private val navigationRoutesParser: NavigationRoutesParser,
    private val mapMatchedRoutesParser: MapMatchingMatchParser,
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

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun getRoute(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        callback: NavigationRouterCallback,
    ): Long {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val routeUrl = routeOptions.toUrl(accessToken).toString()
        return requestRoute(
            routeUrl = routeUrl,
            routeOptionsForCallback = routeOptions,
            callback = object : RouteRequestCallback<NavigationRouteParsingSuccessfulResult> {
                override fun onSuccess(
                    result: NavigationRouteParsingSuccessfulResult,
                    routerOrigin: String,
                ) = callback.onRoutesReady(result.routes, routerOrigin)
                override fun onFailure(failures: List<RouterFailure>, routeOptions: RouteOptions) =
                    callback.onFailure(failures, routeOptions)
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) =
                    callback.onCanceled(routeOptions, routerOrigin)
            },
            responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
            parseResponse = navigationRoutesParser::parseDirectionsResponse,
            performanceSectionName = "RouterWrapper#getRoute()",
            logRequestMessage = "requesting route for",
            logResultMessage = "received result from router.getRoute for",
        ) { routeCallback ->
            router.getRoute(
                routeUrl,
                GetRouteOptions(null),
                signature.toNativeSignature(),
                routeCallback,
            )
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)
    override fun getRouteMapMatched(
        mapMatchingOptions: MapMatchingOptions,
        signature: GetRouteSignature,
        callback: MapMatchingAPICallback,
    ): Long {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val matchingUri = mapMatchingOptions.toURL(accessToken)
        val routeOptionsForCallback = mapMatchingOptions.toRouteOptionsForCallback()
        return requestRoute(
            routeUrl = matchingUri,
            routeOptionsForCallback = routeOptionsForCallback,
            callback = object : RouteRequestCallback<MapMatchingMatchParsingSuccessfulResult> {
                override fun onSuccess(
                    result: MapMatchingMatchParsingSuccessfulResult,
                    routerOrigin: String,
                ) = callback.success(MapMatchingSuccessfulResult(result.matches))
                override fun onFailure(failures: List<RouterFailure>, routeOptions: RouteOptions) =
                    callback.failure(MapMatchingFailure())
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) =
                    callback.onCancel()
            },
            responseOriginAPI = ResponseOriginAPI.MAP_MATCHING_API,
            parseResponse = mapMatchedRoutesParser::parseMapMatchedResponse,
            performanceSectionName = "RouterWrapper#getRouteMapMatched()",
            logRequestMessage = "requesting map-matched route for",
            logResultMessage = "received result from router.getRouteMapMatched for",
        ) { routeCallback ->
            router.getRouteMapMatched(matchingUri, GetRouteOptions(null), routeCallback)
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun <T> requestRoute(
        routeUrl: String,
        routeOptionsForCallback: RouteOptions,
        callback: RouteRequestCallback<T>,
        @ResponseOriginAPI responseOriginAPI: String,
        parseResponse: suspend (ResponseToParse) -> Result<T>,
        performanceSectionName: String,
        logRequestMessage: String,
        logResultMessage: String,
        nativeInvoke: ((Expected<List<RouterError>, DataRef>, RouterOrigin) -> Unit) -> Long,
    ): Long {
        val urlWithoutToken = URL(routeUrl.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM))
        logI(LOG_CATEGORY) { "$logRequestMessage $urlWithoutToken" }
        val originRouter = router
        var callbackInvoked = false
        val section = PerformanceTracker.asyncSectionStarted(performanceSectionName)
        var requestId: Long = 0L

        requestId = nativeInvoke { result, origin ->
            callbackInvoked = true
            logD(LOG_CATEGORY) { "$logResultMessage $urlWithoutToken; origin: $origin" }
            mainJobControl.scope.launch {
                PerformanceTracker.asyncSectionCompleted(section)
                endRouteRequest(
                    requestId,
                    routeOptionsForCallback,
                    routeUrl,
                    urlWithoutToken,
                    originRouter,
                    result,
                    origin,
                    RouteRequestEnder(requestId, callback),
                    responseOriginAPI,
                    parseResponse,
                )
            }
        }
        if (!callbackInvoked) {
            activeRouteRequests[requestId] = OngoingRequest(
                null,
                {
                    PerformanceTracker.asyncSectionCompleted(section)
                    callback.onCanceled(
                        routeOptionsForCallback,
                        com.mapbox.navigation.base.route.RouterOrigin.OFFLINE,
                    )
                },
            )
        }
        return requestId
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

    private fun removeActiveRequest(requestId: Long) {
        activeRouteRequests[requestId]?.let {
            it.onCancel()
            activeRouteRequests.remove(requestId)
            it.parsingJob?.cancel()
        }
    }

    override fun cancelRouteRequest(requestId: Long) {
        removeActiveRequest(requestId)
        router.cancelRouteRequest(requestId)
    }

    override fun cancelMapMatchedRouteRequest(requestId: Long) {
        removeActiveRequest(requestId)
        router.cancelRouteMapMatchedRequest(requestId)
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

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun <T> endRouteRequest(
        id: Long?,
        routeOptions: RouteOptions,
        routeUrl: String,
        urlWithoutToken: URL,
        originRouter: RouterInterface,
        result: Expected<List<RouterError>, DataRef>,
        origin: RouterOrigin,
        requestEnder: RouteRequestCallback<T>,
        @ResponseOriginAPI responseOriginAPI: String,
        parseResponse: suspend (ResponseToParse) -> Result<T>,
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
                        parseResponse(
                            ResponseToParse(
                                responseBody,
                                routeUrl,
                                origin.mapToSdkRouteOrigin(),
                                responseOriginAPI,
                            ),
                        ).onSuccess { response ->
                            val routeOrigin = origin.mapToSdkRouteOrigin()
                            logI("Routes parsing completed", LOG_CATEGORY)
                            requestEnder.onSuccess(response, routeOrigin)
                        }.onFailure { throwable ->
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
                { dataRef ->
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
                            route.internalRefreshRoute(
                                refreshResponse = dataRef,
                                legIndex = refreshOptions.legIndex,
                                legGeometryIndex = routeRefreshRequestData.legGeometryIndex ?: 0,
                                responseTimeElapsedSeconds = responseTimeElapsedSeconds,
                                experimentalProperties = routeRefreshRequestData
                                    .experimentalProperties,
                            )
                        }.fold(
                            { refreshedRoute ->
                                callback.onRefreshReady(refreshedRoute, dataRef)
                            },
                            { throwable ->
                                callback.onFailure(
                                    NavigationRouterRefreshError(
                                        "failed for response: $dataRef",
                                        throwable,
                                    ),
                                )
                            },

                        )
                    }
                },
            )
        }
    }

    private inner class RouteRequestEnder<T>(
        private val id: Long?,
        private val callback: RouteRequestCallback<T>,
    ) : RouteRequestCallback<T> {

        override fun onSuccess(result: T, routerOrigin: String) {
            if (removeRequest()) {
                callback.onSuccess(result, routerOrigin)
            }
        }

        override fun onFailure(failures: List<RouterFailure>, routeOptions: RouteOptions) {
            if (removeRequest()) {
                callback.onFailure(failures, routeOptions)
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

        override fun onRefreshReady(route: NavigationRoute, refreshResponse: DataRef) {
            if (removeRequest()) {
                callback.onRefreshReady(route, refreshResponse)
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
