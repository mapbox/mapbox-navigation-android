package com.mapbox.navigation.testing.router

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestOrResponse
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.common.HttpServiceInterceptorRequestContinuation
import com.mapbox.common.HttpServiceInterceptorResponseContinuation
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

private const val LOG_CATEGORY = "CustomRouterRule"

/**
 * Creates an instance of [CustomRouterRule].
 * Use it as a JUnit rule in your instrumentation tests.
 */
@ExperimentalMapboxNavigationAPI
fun createNavigationRouterRule(): CustomRouterRule {
    return MapboxNavigationRouterRule()
}

/**
 * The rule intercepts online route requests from Navigation Core Framework.
 * Use [createNavigationRouterRule] to create an instance of the rule.
 *
 * The local web server forwards requests from Navigation SDK to [MapboxNavigationTestRouter] and
 * [MapboxNavigationTestRouteRefresher] provided by the SDK's user. When a user provided
 * components returns a route or an error, the rule converts it to a response for Nav SDK.
 */
@ExperimentalMapboxNavigationAPI
interface CustomRouterRule : TestRule {

    /**
     * Use this method to set handler of route requests sent by the Nav SDK.
     * @param router a router which will serve Nav SDK's route requests.
     */
    fun setRouter(router: MapboxNavigationTestRouter)

    /**
     * Use this method to set handler of route refresh requests sent by the Nav SDK.
     */
    fun setRouteRefresher(refresher: MapboxNavigationTestRouteRefresher)
}

@ExperimentalMapboxNavigationAPI
private class MapboxNavigationRouterRule : TestWatcher(), CustomRouterRule {

    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var router: MapboxNavigationTestRouter = DefaultRouter()
    private var refresher: MapboxNavigationTestRouteRefresher = DefaultRefresher()

    override fun setRouter(router: MapboxNavigationTestRouter) {
        this.router = router
    }

    override fun setRouteRefresher(refresher: MapboxNavigationTestRouteRefresher) {
        this.refresher = refresher
    }

    init {
        initDispatcher()
    }

    private fun initDispatcher() {
        logD(LOG_CATEGORY) {
            "Setting up http service interceptor"
        }
        HttpServiceFactory.setHttpServiceInterceptor(
            object : HttpServiceInterceptorInterface {
                override fun onRequest(
                    request: HttpRequest,
                    continuation: HttpServiceInterceptorRequestContinuation,
                ) {
                    scope.launch {
                        interceptRequest(request, continuation)
                    }
                }

                override fun onResponse(
                    response: HttpResponse,
                    continuation: HttpServiceInterceptorResponseContinuation,
                ) {
                    continuation.run(response)
                }
            },
        )
    }

    override fun starting(description: Description?) {
    }

    override fun finished(description: Description?) {
        HttpServiceFactory.setHttpServiceInterceptor(null)
        scope.cancel("CustomRouterRule has finished")
    }

    private suspend fun interceptRequest(
        request: HttpRequest,
        continuation: HttpServiceInterceptorRequestContinuation,
    ) {
        val response = processRequest(router, refresher, request.url)
            .toHttpResponse(request)
        if (response != null) {
            logI(LOG_CATEGORY) {
                "Test response is provided for ${request.url}"
            }
            continuation.run(HttpRequestOrResponse(response))
        } else {
            continuation.run(HttpRequestOrResponse(request))
        }
    }

    private fun RequestProcessingResult.toHttpResponse(
        request: HttpRequest,
    ): HttpResponse? = when (this) {
        is RequestProcessingResult.Failure -> HttpResponse(
            0,
            request,
            ExpectedFactory.createValue(
                HttpResponseData(
                    HashMap(),
                    code,
                    body.toByteArray(),
                ),
            ),
        )

        is RequestProcessingResult.GetRouteResponse -> {
            HttpResponse(
                0,
                request,
                ExpectedFactory.createValue(
                    HttpResponseData(
                        HashMap(),
                        200,
                        response.toJson().toByteArray(),
                    ),
                ),
            )
        }

        is RequestProcessingResult.RefreshRouteResponse -> {
            HttpResponse(
                0,
                request,
                ExpectedFactory.createValue(
                    HttpResponseData(
                        HashMap(),
                        200,
                        response.toJson().toByteArray(),
                    ),
                ),
            )
        }

        RequestProcessingResult.RequestNotSupported -> null
    }
}
