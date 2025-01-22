package com.mapbox.navigation.core.mapmatching

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.route.createMatchedRoutes
import com.mapbox.navigation.base.route.MapMatchingMatch
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private const val LOG_TAG = "MapMatchingAPI"

@ExperimentalPreviewMapboxNavigationAPI
@UiThread
internal class MapMatchingAPI(
    private val serialisationDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    httpServiceFactory: () -> HttpServiceInterface,
    private val sdkInformation: SdkInformation,
    private val getCurrentAccessToken: () -> String,
    private val skuTokenProvider: UrlSkuTokenProvider,
) {

    private val httpService by lazy { httpServiceFactory() }
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())
    private val runningResponseProcessing = mutableMapOf<Long, Job>()
    private val runningRequests = mutableSetOf<Long>()

    fun requestMapMatching(options: MapMatchingOptions, callback: MapMatchingAPICallback): Long {
        // TODO: NAVAND-1732 use Mapbox Map Matching API via NN
        val url = options.toURL(getCurrentAccessToken()).let {
            skuTokenProvider.obtainUrlWithSkuToken(URL(it)).toString()
        }
        val request = HttpRequest.Builder()
            .url(url)
            .sdkInformation(sdkInformation)
            .headers(HashMap())
            .method(HttpMethod.GET)
            .build()
        return requestWithRunningRequestsTracking(
            request,
        ) { response ->
            scope.launch {
                response.result
                    .onError {
                        handleError(it, response, callback)
                    }
                    .onValue { httpResponseData ->
                        if (httpResponseData.code != 200) {
                            logE(LOG_TAG) {
                                "request ${response.requestId}: " +
                                    "http code ${httpResponseData.code}," +
                                    "body: ${String(httpResponseData.data)}"
                            }
                            handleHttpError(callback)
                        } else {
                            logI(LOG_TAG) {
                                "request ${response.requestId}:" +
                                    " http code ${httpResponseData.code}" +
                                    ", parsing response body"
                            }
                            handleSuccessfulResult(
                                response,
                                httpResponseData,
                                callback,
                            )
                        }
                    }
            }
        }.also {
            logI(LOG_TAG) {
                "Map Matching request $it: ${options.toURL("***")}"
            }
        }
    }

    fun cancel(requestId: Long) {
        if (runningRequests.remove(requestId)) {
            httpService.cancelRequest(requestId) { }
        }
        runningResponseProcessing.remove(requestId)?.cancel()
    }

    fun cancelAll() {
        runningRequests.toList().forEach {
            cancel(it)
        }
        runningResponseProcessing.keys.forEach {
            cancel(it)
        }
    }

    private fun CoroutineScope.trackRunningResponseProcessing(
        requestId: Long,
        block: suspend () -> Unit,
    ) {
        runningResponseProcessing[requestId] = launch {
            try {
                block()
            } finally {
                runningResponseProcessing.remove(requestId)
            }
        }
    }

    private fun requestWithRunningRequestsTracking(
        request: HttpRequest,
        callback: HttpResponseCallback,
    ): Long {
        var currentRequestIsRunning = true
        return httpService.request(
            request,
        ) { response ->
            currentRequestIsRunning = false
            runningRequests.remove(response.requestId)
            callback.run(response)
        }.also {
            if (currentRequestIsRunning) {
                runningRequests.add(it)
            }
        }
    }

    private fun CoroutineScope.handleSuccessfulResult(
        response: HttpResponse,
        responseData: HttpResponseData,
        callback: MapMatchingAPICallback,
    ) {
        trackRunningResponseProcessing(response.requestId) {
            try {
                val routes = withContext(serialisationDispatcher) {
                    parseMapMatchingResponseToNavigationRoutes(
                        response.request.url,
                        responseData,
                    ).getValueOrElse {
                        "request ${response.requestId}: error parsing response $it"
                        emptyList()
                    }
                }
                if (routes.isEmpty()) {
                    callback.failure(MapMatchingFailure())
                } else {
                    logI(LOG_TAG) {
                        "Map Matching response parsing completed:" +
                            " ${routes.map { it.navigationRoute.id }}"
                    }
                    callback.success(MapMatchingSuccessfulResult(routes))
                }
            } catch (ce: CancellationException) {
                callback.onCancel()
                throw ce
            }
        }
    }

    @WorkerThread
    private fun parseMapMatchingResponseToNavigationRoutes(
        requestUrl: String,
        responseData: HttpResponseData,
    ): Expected<Throwable, List<MapMatchingMatch>> {
        // TODO: NAVAND-1733 parse via [RouteParsingManager]
        val responseRaw = String(responseData.data)
        return createMatchedRoutes(
            responseRaw,
            requestUrl,
        )
    }

    private fun handleError(
        it: HttpRequestError,
        response: HttpResponse,
        callback: MapMatchingAPICallback,
    ) {
        if (it.type == HttpRequestErrorType.REQUEST_CANCELLED) {
            logI(LOG_TAG) {
                "request ${response.request} was cancelled"
            }
            callback.onCancel()
        } else {
            logE(LOG_TAG) {
                "request ${response.requestId}: error ${it.type}, ${it.message}"
            }
            callback.failure(MapMatchingFailure())
        }
    }

    private fun handleHttpError(callback: MapMatchingAPICallback) {
        callback.failure(MapMatchingFailure())
    }
}
