package com.mapbox.navigation.testing.http

import android.annotation.SuppressLint
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.DownloadOptions
import com.mapbox.common.DownloadStatusCallback
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.ResultCallback
import com.mapbox.common.UploadOptions
import com.mapbox.common.UploadStatusCallback
import org.junit.Assert.fail

typealias CommonHttpRequestHandler = (
    requestId: Long,
    request: HttpRequest,
    callback: HttpResponseCallback
) -> Unit

@SuppressLint("RestrictedApi")
fun createTestHttpService(
    requestHandler: CommonHttpRequestHandler = { _, _, _ ->
        TODO("not provided")
    }
): HttpServiceInterface {
    return object : HttpServiceInterface {

        private var nextRequestId = 0L
        private val notCompletedRequests =
            mutableMapOf<Long, Pair<HttpRequest, HttpResponseCallback>>()

        override fun setInterceptor(interceptor: HttpServiceInterceptorInterface?) {
            TODO("Not yet implemented")
        }

        override fun setMaxRequestsPerHost(max: Byte) {
            TODO("Not yet implemented")
        }

        override fun request(request: HttpRequest, callback: HttpResponseCallback): Long {
            val requestId = nextRequestId++
            notCompletedRequests[requestId] = Pair(request, callback)
            requestHandler(
                requestId,
                request,
                HttpResponseCallback {
                    notCompletedRequests.remove(requestId)?.second?.run(it)
                }
            )
            return requestId
        }

        override fun cancelRequest(id: Long, callback: ResultCallback) {
            notCompletedRequests.remove(id)?.let {
                it.second.run(
                    HttpResponse(
                        id,
                        it.first,
                        ExpectedFactory.createError(
                            HttpRequestError(
                                HttpRequestErrorType.REQUEST_CANCELLED,
                                "cancelled"
                            )
                        )
                    )
                )
            } ?: fail(
                "Cancelled request id=$id which is not running." +
                    " Running requests: ${notCompletedRequests.keys}"
            )
            callback.run(true)
        }

        override fun supportsKeepCompression(): Boolean {
            TODO("Not yet implemented")
        }

        override fun download(options: DownloadOptions, callback: DownloadStatusCallback): Long {
            TODO("Not yet implemented")
        }

        override fun upload(options: UploadOptions, callback: UploadStatusCallback): Long {
            TODO("Not yet implemented")
        }

        override fun cancelUpload(id: Long, callback: ResultCallback) {
            TODO("Not yet implemented")
        }
    }
}