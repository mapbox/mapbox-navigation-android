package com.mapbox.navigation.testing.ui.http

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestOrResponse
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.common.HttpServiceInterceptorRequestContinuation
import com.mapbox.common.HttpServiceInterceptorResponseContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

fun HttpServiceEventsObserver.billingRequests(): List<HttpServiceEvent.Request> {
    return eventsFlow.replayCache
        .filterIsInstance<HttpServiceEvent.Request>()
        .filter { it.isBillingEvent }
}

fun HttpServiceEventsObserver.billingRequestsFlow(): Flow<HttpServiceEvent.Request> {
    return onRequestEventsFlow
        .filter { it.isBillingEvent }
}

/**
 * Records every HTTP request and response that goes through the common HTTP service.
 *
 * @param respondLocallyTo requests that match this predicate are still recorded, but they are
 * answered locally with an empty 200 response and never reach the network. All other requests
 * are sent as usual.
 */
class HttpServiceEventsObserver(
    private val respondLocallyTo: (HttpServiceEvent.Request) -> Boolean = { false },
) : HttpServiceInterceptorInterface {

    private val _eventsFlow = MutableSharedFlow<HttpServiceEvent>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE
    )

    val eventsFlow: SharedFlow<HttpServiceEvent>
        get() = _eventsFlow

    val onRequestEventsFlow: Flow<HttpServiceEvent.Request> = eventsFlow
        .mapNotNull { it as? HttpServiceEvent.Request }

    override fun onRequest(
        request: HttpRequest,
        continuation: HttpServiceInterceptorRequestContinuation
    ) {
        val event = HttpServiceEvent.Request(request)
        onEvent(event)
        if (respondLocallyTo(event)) {
            continuation.run(HttpRequestOrResponse.valueOf(successfulEmptyResponse(request)))
        } else {
            continuation.run(HttpRequestOrResponse.valueOf(request))
        }
    }

    override fun onResponse(
        response: HttpResponse,
        continuation: HttpServiceInterceptorResponseContinuation
    ) {
        onEvent(HttpServiceEvent.Response(response))
        continuation.run(response)
    }

    private fun onEvent(event: HttpServiceEvent) {
        _eventsFlow.tryEmit(event)
    }

    companion object {
        fun register(
            respondLocallyTo: (HttpServiceEvent.Request) -> Boolean = { false },
        ): HttpServiceEventsObserver {
            return HttpServiceEventsObserver(respondLocallyTo).also {
                HttpServiceFactory.setHttpServiceInterceptor(it)
            }
        }

        private fun successfulEmptyResponse(request: HttpRequest): HttpResponse {
            return HttpResponse(
                0,
                request,
                ExpectedFactory.createValue(HttpResponseData(hashMapOf(), 200, ByteArray(0))),
            )
        }

        fun unregister() {
            HttpServiceFactory.setHttpServiceInterceptor(null)
        }
    }
}
