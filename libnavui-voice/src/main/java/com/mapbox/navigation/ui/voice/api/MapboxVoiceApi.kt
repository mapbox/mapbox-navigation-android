package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.VoiceAction
import com.mapbox.navigation.ui.voice.VoiceAction.PrepareTypeAndAnnouncement
import com.mapbox.navigation.ui.voice.VoiceAction.PrepareVoiceRequest
import com.mapbox.navigation.ui.voice.VoiceAction.ProcessVoiceResponse
import com.mapbox.navigation.ui.voice.VoiceProcessor
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceRequest
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceResponse
import com.mapbox.navigation.ui.voice.VoiceResult.VoiceTypeAndAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.model.VoiceState.VoiceError
import java.io.File

/**
 * Implementation of [VoiceApi] allowing you to retrieve voice instructions.
 */
internal class MapboxVoiceApi(
    private val speechProvider: MapboxSpeechProvider,
    private val speechFileProvider: MapboxSpeechFileProvider
) : VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceState]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     */
    override suspend fun retrieveVoiceFile(voiceInstruction: VoiceInstructions): VoiceState {
        return processAction(PrepareTypeAndAnnouncement(voiceInstruction))
    }

    /**
     * Given the [SpeechAnnouncement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    override fun clean(announcement: SpeechAnnouncement) {
        announcement.file?.let {
            speechFileProvider.delete(it)
        }
    }

    private suspend fun processAction(action: VoiceAction): VoiceState {
        return when (val result = VoiceProcessor.process(action)) {
            is VoiceTypeAndAnnouncement -> {
                when (result) {
                    is VoiceTypeAndAnnouncement.Success -> processAction(
                        PrepareVoiceRequest(result.typeAndAnnouncement)
                    )
                    is VoiceTypeAndAnnouncement.Failure -> VoiceError(result.error)
                }
            }
            is VoiceRequest -> {
                when (result) {
                    is VoiceRequest.Success -> processVoiceRequest(result)
                }
            }
            is VoiceResponse -> {
                when (result) {
                    is VoiceResponse.Success ->
                        VoiceState.VoiceFile(speechFileProvider.generateVoiceFileFrom(result.data))
                    is VoiceResponse.Failure -> VoiceError(
                        "code: ${result.responseCode}, error: ${result.error}"
                    )
                }
            }
        }
    }

    private suspend fun processVoiceRequest(result: VoiceRequest.Success): VoiceState {
        return when (val speechResult = speechProvider.enqueueCall(result)) {
            is VoiceState.VoiceResponse -> {
                processAction(ProcessVoiceResponse(speechResult.response))
            }
            else -> speechResult
        }
    }
}
