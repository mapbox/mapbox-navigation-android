package com.mapbox.navigation.navigator

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.DownloadOptions
import com.mapbox.common.DownloadStatusCallback
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.ResultCallback
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.HashMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

internal class NavigationOkHttpService(
    private val httpClientBuilder: OkHttpClient.Builder,
    private val logger: Logger? = null
) : HttpServiceInterface() {

    companion object {
        internal const val HEADER_ENCODING = "Accept-Encoding"
        internal const val GZIP = "gzip, deflate"
        private const val LOG_TAG = "NavigationOkHttpService"
    }

    private val lock = ReentrantLock()

    private val callMap = hashMapOf<Long, Call>()

    private val httpClient: OkHttpClient by lazy {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor(
                HttpLoggingInterceptor.Logger { message ->
                    logger?.d(
                        tag = Tag(LOG_TAG),
                        msg = Message(message)
                    )
                }
            ).setLevel(HttpLoggingInterceptor.Level.BASIC)

            httpClientBuilder.addInterceptor(interceptor)
        }
        httpClientBuilder.build()
    }

    override fun setMaxRequestsPerHost(max: Byte) {
        httpClient.dispatcher().maxRequestsPerHost = max.toInt()
    }

    override fun supportsKeepCompression(): Boolean {
        return true
    }

    override fun request(request: HttpRequest, callback: HttpResponseCallback): Long {
        val requestBuilder = Request.Builder()
        val resourceUrl: String = request.url
        requestBuilder.url(resourceUrl).tag(resourceUrl.toLowerCase(Locale.US))
        requestBuilder.headers(request.headers.toHeaders())
        if (request.keepCompression) {
            // Adding this header manually means okhttp will not automatically decompress the data
            requestBuilder.addHeader(
                HEADER_ENCODING,
                GZIP
            )
        }

        lock.lock()
        var id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        while (callMap.containsKey(id)) {
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        }
        val call = httpClient.newCall(requestBuilder.build())
        call.enqueue(
            HttpCallback(
                id,
                request,
                callback
            )
        )
        callMap[id] = call
        lock.unlock()
        return id
    }

    override fun cancelRequest(id: Long, callback: ResultCallback) {
        lock.lock()
        val call = callMap.remove(id)
        if (call != null) {
            call.cancel()
            callback.run(false)
        } else {
            callback.run(true)
        }
        lock.unlock()
    }

    override fun download(options: DownloadOptions, callback: DownloadStatusCallback): Long {
        // To change body of created functions use File | Settings | File Templates.
        TODO("not implemented")
    }

    internal inner class HttpCallback constructor(
        private val id: Long,
        private val request: HttpRequest,
        private val callback: HttpResponseCallback
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                HttpRequestError(HttpRequestErrorType.OTHER_ERROR, e.message.toString())
            )
            runCallback(result)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val responseBody = response.body()
                if (responseBody == null) {
                    val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                        HttpRequestError(HttpRequestErrorType.OTHER_ERROR, "empty body")
                    )
                    runCallback(result)
                    return
                }
                val responseData = HttpResponseData(
                    response.headers().toHashMap(),
                    response.code().toLong(),
                    responseBody.bytes()
                )
                val result =
                    ExpectedFactory.createValue<HttpResponseData, HttpRequestError>(responseData)
                runCallback(result)
            } catch (exception: Exception) {
                val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                    HttpRequestError(HttpRequestErrorType.OTHER_ERROR, exception.message.toString())
                )
                runCallback(result)
            }
        }

        private fun runCallback(result: Expected<HttpResponseData, HttpRequestError>) {
            lock.lock()
            callMap.remove(id)?.let {
                if (result.isError) {
                    logger?.e(msg = Message(result.error?.message.toString()))
                }
                callback.run(HttpResponse(request, result))
            }
            lock.unlock()
        }
    }
}

internal fun HashMap<String, String>.toHeaders(): Headers {
    val builder = Headers.Builder()
    for ((key, value) in this) {
        builder.add(key, value)
    }
    return builder.build()
}

internal fun Headers.toHashMap(): HashMap<String, String> {
    val outputMap = HashMap<String, String>()
    for (i in 0 until this.size()) {
        outputMap[this.name(i)] = this.value(i)
    }
    return outputMap
}
