package com.mapbox.navigation.ui.utils.internal.network

import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.navigation.utils.internal.logD

class LoggingInterceptor : HttpServiceInterceptorInterface {
    override fun onRequest(request: HttpRequest): HttpRequest {
        logD(
            ">> onRequest ${request.url}|${request.headers}",
            LOG_CAT
        )
        return request
    }

    override fun onDownload(download: DownloadOptions): DownloadOptions {
        logD(
            ">> onDownload ${download.request.url}|${download.request.headers}",
            LOG_CAT
        )
        return download
    }

    override fun onResponse(response: HttpResponse): HttpResponse {
        logD(
            "<< onResponse ${response.result?.value?.code} ${response.request.url}",
            LOG_CAT
        )
        return response
    }

    companion object {
        private const val LOG_CAT = "LoggingInterceptor"
    }
}
