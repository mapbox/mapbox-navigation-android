package com.mapbox.navigation.testing.ui.http

import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
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

    override fun onRequest(request: HttpRequest): HttpRequest {
        onEvent(HttpServiceEvent.Request(request))
        return request
    }

    override fun onDownload(download: DownloadOptions): DownloadOptions {
        onEvent(HttpServiceEvent.Download(download))
        return download
    }

    override fun onResponse(response: HttpResponse): HttpResponse {
        onEvent(HttpServiceEvent.Response(response))
        return response
    }

    private fun onEvent(event: HttpServiceEvent) {
        _eventsFlow.tryEmit(event)
    }

    companion object {
        fun register(): HttpServiceEventsObserver {
            return HttpServiceEventsObserver().also {
                HttpServiceFactory.getInstance().setInterceptor(it)
            }
        }

        fun unregister() {
            HttpServiceFactory.getInstance().setInterceptor(null)
        }
    }
}
