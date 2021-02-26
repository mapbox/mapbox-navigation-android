package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.api.speech.v1.MapboxSpeech
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceRequest
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceResponse
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceTypeAndAnnouncement
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement
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
            is VoiceAction.PrepareTypeAndAnnouncement ->
                prepareTypeAndAnnouncement(action.instruction)
            is VoiceAction.PrepareVoiceRequest -> prepareRequest(action.typeAndAnnouncement)
            is VoiceAction.ProcessVoiceResponse -> processResponse(action.response)
        }
    }

    private fun prepareTypeAndAnnouncement(
        instruction: VoiceInstructions
    ): VoiceTypeAndAnnouncement {
        val announcement = instruction.announcement()
        val ssmlAnnouncement = instruction.ssmlAnnouncement()
        val (type, instruction) =
            if (ssmlAnnouncement != null && !ssmlAnnouncement.isNullOrBlank()) {
                Pair(SSML_TYPE, ssmlAnnouncement)
            } else if (announcement != null && !announcement.isNullOrBlank()) {
                Pair(TEXT_TYPE, announcement)
            } else {
                return VoiceTypeAndAnnouncement.Failure(
                    "VoiceInstructions ssmlAnnouncement / announcement can't be null or blank"
                )
            }
        return VoiceTypeAndAnnouncement.Success(TypeAndAnnouncement(type, instruction))
    }

    private fun prepareRequest(typeAndAnnouncement: TypeAndAnnouncement): VoiceRequest {
        val requestBuilder = MapboxSpeech.builder()
            .textType(typeAndAnnouncement.type)
            .instruction(typeAndAnnouncement.announcement)
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
