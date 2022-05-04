package com.mapbox.navigation.ui.utils.internal.network

import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceInterceptorInterface
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class HttpServiceInterceptorChain : HttpServiceInterceptorInterface {
    private val chain: Queue<HttpServiceInterceptorInterface> = ConcurrentLinkedQueue()

    fun add(interceptor: HttpServiceInterceptorInterface): HttpServiceInterceptorChain {
        chain.add(interceptor)
        return this
    }

    fun remove(interceptor: HttpServiceInterceptorInterface): HttpServiceInterceptorChain {
        chain.remove(interceptor)
        return this
    }

    fun removeIf(predicate: (HttpServiceInterceptorInterface) -> Boolean) {
        chain.removeAll(predicate)
    }

    override fun onRequest(request: HttpRequest): HttpRequest {
        return chain.fold(request) { req, interceptor ->
            interceptor.onRequest(req)
        }
    }

    override fun onDownload(download: DownloadOptions): DownloadOptions {
        return chain.fold(download) { opt, interceptor ->
            interceptor.onDownload(opt)
        }
    }

    override fun onResponse(response: HttpResponse): HttpResponse {
        return chain.fold(response) { res, interceptor ->
            interceptor.onResponse(res)
        }
    }
}
