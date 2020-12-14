package com.mapbox.navigation.ui.maps.internal

import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal object FetchImageProcessor {

    private const val USER_AGENT_KEY = "User-Agent"
    private const val USER_AGENT_VALUE = "MapboxJava/"

    private val okHttpClient = OkHttpClient.Builder().build()

    internal suspend fun fetchImage(urlToFetch: String): ImageResponse =
        withContext(ThreadController.IODispatcher) {
            suspendCoroutine {
                val req = Request.Builder()
                    .url(urlToFetch)
                    .header(USER_AGENT_KEY, USER_AGENT_VALUE)
                    .build()
                val response = okHttpClient.newCall(req).execute()
                okHttpClient.newCall(req).enqueue(
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            resumeCoroutine(ImageResponse.Failure(e.message))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            ifNonNull(response.body()) { body ->
                                resumeCoroutine(ImageResponse.Success(body.bytes()))
                            } ?: resumeCoroutine(ImageResponse.Failure("Data stream is null"))
                        }

                        private fun resumeCoroutine(result: ImageResponse) {
                            it.resumeWith(Result.success(result))
                        }
                    }
                )
            }
        }
}
