package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.speech.v1.MapboxSpeech
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.ui.voice.VoiceResult
import com.mapbox.navigation.ui.voice.model.VoiceState
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class MapboxSpeechProvider(
    private val accessToken: String,
    private val language: String,
    private val urlSkuTokenProvider: UrlSkuTokenProvider
) {

    suspend fun enqueueCall(request: VoiceResult.VoiceRequest.Success): VoiceState {
        val mapboxSpeech = setupMapboxSpeech(request)
        return suspendCoroutine { continuation ->
            mapboxSpeech.enqueueCall(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    continuation.resume(VoiceState.VoiceResponse(response))
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    continuation.resume(VoiceState.VoiceError(t.localizedMessage ?: "Unknown"))
                }
            })
        }
    }

    private fun setupMapboxSpeech(request: VoiceResult.VoiceRequest.Success): MapboxSpeech {
        return request.requestBuilder
            .accessToken(accessToken)
            .language(language)
            .interceptor {
                val httpUrl = it.request().url()
                val skuUrl =
                    urlSkuTokenProvider.obtainUrlWithSkuToken(
                        httpUrl.toString(),
                        httpUrl.querySize()
                    )
                it.proceed(it.request().newBuilder().url(skuUrl).build())
            }
            .build()
    }
}
