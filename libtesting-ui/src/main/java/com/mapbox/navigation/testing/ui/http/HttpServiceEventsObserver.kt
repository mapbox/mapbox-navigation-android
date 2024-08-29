package com.mapbox.navigation.testing.ui.http

import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestOrResponse
import com.mapbox.common.HttpResponse
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
        .filterIsInstance(HttpServiceEvent.Request::class.java)
        .filter { it.isBillingEvent }
}

fun HttpServiceEventsObserver.billingRequestsFlow(): Flow<HttpServiceEvent.Request> {
    return onRequestEventsFlow
        .filter { it.isBillingEvent }
}

class HttpServiceEventsObserver : HttpServiceInterceptorInterface {

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
        onEvent(HttpServiceEvent.Request(request))
        continuation.run(HttpRequestOrResponse.valueOf(request))
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
        fun register(): HttpServiceEventsObserver {
            return HttpServiceEventsObserver().also {
                HttpServiceFactory.setHttpServiceInterceptor(it)
            }
        }

        fun unregister() {
            HttpServiceFactory.setHttpServiceInterceptor(null)
        }
    }
}
