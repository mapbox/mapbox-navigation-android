package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import okhttp3.ResponseBody
import retrofit2.Response

internal sealed class VoiceAction {

    data class PrepareVoiceRequest(
        val instruction: VoiceInstructions
    ) : VoiceAction()

    data class ProcessVoiceResponse(
        val response: Response<ResponseBody>
    ) : VoiceAction()
}
