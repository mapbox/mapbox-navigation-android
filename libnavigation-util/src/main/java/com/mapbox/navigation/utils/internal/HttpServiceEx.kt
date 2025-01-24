package com.mapbox.navigation.utils.internal

import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun HttpServiceInterface.request(request: HttpRequest):
    Expected<HttpRequestError, HttpResponseData> =
    suspendCancellableCoroutine { continuation ->
        val requestId = request(request) { response ->
            if (response.result.error?.type != HttpRequestErrorType.REQUEST_CANCELLED) {
                continuation.resume(response.result)
            }
        }
        continuation.invokeOnCancellation {
            cancelRequest(requestId) { }
        }
    }
