package com.mapbox.navigation.route.onboard.network

import androidx.annotation.Keep
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigator.BuildConfig
import com.mapbox.navigator.HttpCode
import com.mapbox.navigator.HttpInterface
import com.mapbox.navigator.HttpResponse
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okio.buffer
import okio.sink

@Keep
internal class HttpClient(
    internal val userAgent: String = USER_AGENT,
    private val acceptGzipEncoding: Boolean = false,
    private val logger: Logger? = null,
    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) : HttpInterface() {

    companion object {
        private const val USER_AGENT = "MapboxNavigationNative"

        private const val ERROR_EMPTY_USER_AGENT = "Empty UserAgent is not allowed"
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val GZIP = "gzip"
    }

    private val client: OkHttpClient by lazy {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message ->
                logger?.d(
                    msg = Message(message)
                )
            }).setLevel(HttpLoggingInterceptor.Level.BASIC)

            clientBuilder.addInterceptor(interceptor)
        }

        clientBuilder.build()
    }

    init {
        check(userAgent.isNotEmpty()) {
            ERROR_EMPTY_USER_AGENT
        }
    }

    override fun isGzipped(): Boolean {
        return acceptGzipEncoding
    }

    override fun get(url: String): HttpResponse {
        val requestBuilder = try {
            Request.Builder()
                .addHeader(HEADER_USER_AGENT, userAgent)
                .url(url)
        } catch (e: IllegalArgumentException) {
            return HttpResponse(ByteArray(0), HttpCode.FAILURE)
        }

        if (acceptGzipEncoding) {
            requestBuilder.addHeader(HEADER_ENCODING, GZIP)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val outputStream = ByteArrayOutputStream()
            val result = if (response.isSuccessful) HttpCode.SUCCESS else HttpCode.FAILURE

            response.body()?.let { body ->
                val sink = outputStream.sink().buffer()
                sink.writeAll(body.source())
                sink.close()
            }

            val bytes = outputStream.toByteArray()
            outputStream.close()

            return HttpResponse(bytes, result)
        }
    }
}
