package com.mapbox.navigation.ui.voice

import com.mapbox.api.speech.v1.MapboxSpeech
import okhttp3.ResponseBody

internal sealed class VoiceResult {
    sealed class VoiceRequest : VoiceResult() {
        data class Success(val requestBuilder: MapboxSpeech.Builder) : VoiceRequest()
        data class Failure(val error: String) : VoiceRequest()
    }

    sealed class VoiceResponse : VoiceResult() {
        data class Success(val data: ResponseBody) : VoiceResponse()
        data class Failure(val responseCode: Int, val error: String) : VoiceResponse()
    }
}
