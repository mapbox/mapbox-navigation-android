package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.voice.VoiceAction
import com.mapbox.navigation.ui.voice.VoiceProcessor
import com.mapbox.navigation.ui.voice.VoiceResult
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Mapbox Speech Api that allows you to generate an announcement based on [VoiceInstructions]
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 * @property options [MapboxSpeechApiOptions] (optional)
 */
class MapboxSpeechApi @JvmOverloads constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: MapboxSpeechApiOptions = MapboxSpeechApiOptions.Builder().build()
) : SpeechApi {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var currentVoiceFileJob: Job? = null
    private val voiceAPI = VoiceApiProvider.retrieveMapboxVoiceApi(
        context,
        accessToken,
        language,
        options
    )

    /**
     * Given [VoiceInstructions] the method will try to generate the
     * voice instruction [Announcement] including the synthesized speech mp3 file
     * from Mapbox's API Voice.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     * @see [cancel]
     */
    override fun generate(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        currentVoiceFileJob?.cancel()
        currentVoiceFileJob = mainJobController.scope.launch {
            retrieveVoiceFile(voiceInstruction, callback)
        }
    }

    /**
     * The method stops the process of retrieving the file voice instruction [Announcement]
     * and destroys any related callbacks.
     * @see [generate]
     */
    override fun cancel() {
        currentVoiceFileJob?.cancel()
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
                            // Can't be null as it's checked in retrieveVoiceFile
                            announcement!!,
                            ssmlAnnouncement,
                            result.instructionFile
                        )
                    )
                )
            }
            is VoiceState.VoiceError -> {
                processVoiceAnnouncement(
                    voiceInstruction
                ) { available ->
                    callback.onError(
                        SpeechState.Speech.Error(result.exception),
                        available
                    )
                }
            }
        }
    }

    private suspend fun processVoiceAnnouncement(
        voiceInstruction: VoiceInstructions,
        onAvailable: (SpeechState.Speech.Available) -> Unit
    ) {
        val checkVoiceInstructionsResult =
            VoiceProcessor.process(VoiceAction.PrepareTypeAndAnnouncement(voiceInstruction))
        val typeAndAnnouncement =
            checkVoiceInstructionsResult as VoiceResult.VoiceTypeAndAnnouncement
        when (typeAndAnnouncement) {
            is VoiceResult.VoiceTypeAndAnnouncement.Success -> {
                val announcement = voiceInstruction.announcement()
                val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                val available = SpeechState.Speech.Available(
                    Announcement(
                        // Can't be null as it's checked in processVoiceAnnouncement
                        announcement!!,
                        ssmlAnnouncement,
                        null
                    )
                )
                onAvailable(available)
            }
            is VoiceResult.VoiceTypeAndAnnouncement.Failure -> {
                check(false) {
                    "Invalid state: processVoiceAnnouncement can't produce " +
                        "Failure VoiceTypeAndAnnouncement VoiceResult"
                }
            }
        }
    }
}
