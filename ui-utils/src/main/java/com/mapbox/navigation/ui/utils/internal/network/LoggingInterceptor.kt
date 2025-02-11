package com.mapbox.navigation.ui.utils.internal.network

import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestOrResponse
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.common.HttpServiceInterceptorRequestContinuation
import com.mapbox.common.HttpServiceInterceptorResponseContinuation
import com.mapbox.navigation.utils.internal.logD

class LoggingInterceptor : HttpServiceInterceptorInterface {

    override fun onRequest(
        request: HttpRequest,
        continuation: HttpServiceInterceptorRequestContinuation,
    ) {
        logD(
            ">> onRequest ${request.url}|${request.headers}",
            LOG_CAT,
        )
        continuation.run(HttpRequestOrResponse(request))
    }

    override fun onResponse(
        response: HttpResponse,
        continuation: HttpServiceInterceptorResponseContinuation,
    ) {
        logD(
            "<< onResponse ${response.result.value?.code} ${response.request.url}",
            LOG_CAT,
        )
        continuation.run(response)
    }

    companion object {
        private const val LOG_CAT = "LoggingInterceptor"
    }
}
