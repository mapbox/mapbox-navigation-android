package com.mapbox.services.android.navigation.v5.internal.navigation

import androidx.annotation.Keep
import com.mapbox.navigator.HttpCode
import com.mapbox.navigator.HttpInterface
import com.mapbox.navigator.HttpResponse
import com.mapbox.services.android.navigation.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Okio
import timber.log.Timber

@Keep
internal class HttpClient(
    private val userAgent: String,
    private val acceptGzipEncoding: Boolean = false,
    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) : HttpInterface() {

    companion object {
        private const val ERROR_EMPTY_USER_AGENT = "Empty UserAgent is not allowed"
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val GZIP = "gzip"
    }

    private val client: OkHttpClient by lazy {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor(
                HttpLoggingInterceptor.Logger { message -> Timber.d(message) }
            ).setLevel(HttpLoggingInterceptor.Level.BASIC)

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

    override fun get(url: String, nativeResponse: HttpResponse) {
        val requestBuilder = try {
            Request.Builder()
                .addHeader(HEADER_USER_AGENT, userAgent)
                .url(url)
        } catch (e: IllegalArgumentException) {
            nativeResponse.run(ByteArray(0), HttpCode.FAILURE)
            return
        }

        if (acceptGzipEncoding) {
            requestBuilder.addHeader(HEADER_ENCODING, GZIP)
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                nativeResponse.run(ByteArray(0), HttpCode.FAILURE)
            }

            override fun onResponse(call: Call, response: Response) {
                val outputStream = ByteArrayOutputStream()
                val result = if (response.isSuccessful) HttpCode.SUCCESS else HttpCode.FAILURE

                response.body()?.let { body ->
                    val sink = Okio.buffer(Okio.sink(outputStream))
                    sink.writeAll(body.source())
                    sink.close()
                }

                val bytes = outputStream.toByteArray()
                outputStream.close()

                nativeResponse.run(bytes, result)
            }
        })
    }
}
