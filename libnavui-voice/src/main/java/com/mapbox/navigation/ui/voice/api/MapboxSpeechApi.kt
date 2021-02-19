package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Mapbox Speech Api that allows you to generate an announcement based on [VoiceInstructions]
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 */
class MapboxSpeechApi(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechApi {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var currentVoiceJob: Job? = null
    private val voiceAPI = VoiceApiProvider.retrieveMapboxVoiceApi(context, accessToken, language)

    /**
     * Given [VoiceInstructions] the method will generate the voice instruction [Announcement].
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun generate(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        currentVoiceJob?.cancel()
        currentVoiceJob = mainJobController.scope.launch {
            retrieveVoiceFile(voiceInstruction, callback)
        }
    }

    /**
     * The method stops the process of retrieving the voice instruction [Announcement]
     * and destroys any related callbacks.
     */
    override fun cancel() {
        currentVoiceJob?.cancel()
    }

    /**
     * Given the [Announcement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    override fun clean(announcement: Announcement) {
        voiceAPI.clean(announcement)
    }

    private suspend fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
        callback: SpeechCallback
    ) {
        when (val result = voiceAPI.retrieveVoiceFile(voiceInstruction)) {
            is VoiceState.VoiceResponse -> {
                check(false) {
                    "Invalid state: retrieveVoiceFile can't produce VoiceResponse VoiceState"
                }
            }
            is VoiceState.VoiceFile -> {
                val announcement = voiceInstruction.announcement()
                val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                callback.onAvailable(
                    SpeechState.Speech.Available(
                        Announcement(
                            // Can't be null as it's checked in VoiceProcessor#prepareRequest
                            announcement!!,
                            ssmlAnnouncement,
                            result.instructionFile
                        )
                    )
                )
            }
            is VoiceState.VoiceError -> {
                callback.onError(SpeechState.Speech.Error(result.exception))
            }
        }
    }
}
