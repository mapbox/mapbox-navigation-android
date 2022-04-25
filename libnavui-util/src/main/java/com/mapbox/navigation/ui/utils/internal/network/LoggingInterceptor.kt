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
            "LoggingInterceptor"
        )
        return request
    }

    override fun onDownload(download: DownloadOptions): DownloadOptions {
        logD(
            ">> onDownload ${download.request.url}|${download.request.headers}",
            "LoggingInterceptor"
        )
        return download
    }

    override fun onResponse(response: HttpResponse): HttpResponse {
        logD(
            "<< onResponse ${response.result?.value?.code} ${response.request.url}",
            "LoggingInterceptor"
        )
        return response
    }
}
