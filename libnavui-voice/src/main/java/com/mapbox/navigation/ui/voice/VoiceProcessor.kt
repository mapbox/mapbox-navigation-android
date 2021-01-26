package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.api.speech.v1.MapboxSpeech
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceRequest
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceResponse
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

internal object VoiceProcessor {

    private const val SSML_TYPE = "ssml"
    private const val TEXT_TYPE = "text"

    /**
     * The function takes [VoiceAction] performs business logic and returns [VoiceResult]
     * @param action VoiceAction user specific commands
     * @return VoiceResult
     */
    suspend fun process(action: VoiceAction): VoiceResult =
        withContext(ThreadController.IODispatcher) {
            return@withContext processVoiceAction(action)
        }

    private fun processVoiceAction(action: VoiceAction): VoiceResult {
        return when (action) {
            is VoiceAction.PrepareVoiceRequest -> prepareRequest(action.instruction)
            is VoiceAction.ProcessVoiceResponse -> processResponse(action.response)
        }
    }

    private fun prepareRequest(instruction: VoiceInstructions): VoiceRequest {
        val (type, announcement) = if (instruction.ssmlAnnouncement() != null) {
            Pair(SSML_TYPE, instruction.ssmlAnnouncement())
        } else {
            Pair(TEXT_TYPE, instruction.announcement())
        }
        if (announcement.isNullOrBlank()) {
            return VoiceRequest.Failure(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank"
            )
        }
        val requestBuilder = MapboxSpeech.builder()
            .instruction(announcement)
            .textType(type)
        return VoiceRequest.Success(requestBuilder)
    }

    private fun processResponse(response: Response<ResponseBody>): VoiceResponse {
        return if (response.isSuccessful) {
            response.body()?.let {
                VoiceResponse.Success(it)
            } ?: VoiceResponse.Failure(response.code(), "No data available")
        } else {
            VoiceResponse.Failure(response.code(), response.errorBody()?.string() ?: "Unknown")
        }
    }
}
