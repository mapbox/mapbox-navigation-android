package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
import java.net.URL

sealed class HttpServiceEvent {

    abstract val request: HttpRequest

    val url: URL
        get() = URL(request.url)

    data class Request(override val request: HttpRequest) : HttpServiceEvent()

    data class Download(
        val download: DownloadOptions,
        override val request: HttpRequest = download.request
    ) : HttpServiceEvent()

    data class Response(
        val response: HttpResponse,
        override val request: HttpRequest = response.request
    ) : HttpServiceEvent()
}

class HttpServiceEventsObserver : HttpServiceInterceptorInterface {

    private val _latestEvents = mutableListOf<HttpServiceEvent>()

    val latestEvents: List<HttpServiceEvent>
        get() = _latestEvents

    val onRequestEvents: List<HttpServiceEvent.Request>
        get() = latestEvents.mapNotNull { it as? HttpServiceEvent.Request }

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
        _latestEvents.add(event)
    }

    companion object {
        fun register(): HttpServiceEventsObserver {
            return HttpServiceEventsObserver().also {
                HttpServiceFactory.getInstance().setInterceptor(it)
            }
        }
    }
}
