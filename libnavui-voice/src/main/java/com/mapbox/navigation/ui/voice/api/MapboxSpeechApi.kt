package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.VoiceAction
import com.mapbox.navigation.ui.voice.VoiceProcessor
import com.mapbox.navigation.ui.voice.VoiceResult
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch
import java.util.Locale

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
) {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val voiceAPI = VoiceApiProvider.retrieveMapboxVoiceApi(
        context,
        accessToken,
        language,
        options
    )

    /**
     * Given [VoiceInstructions] the method will try to generate the
     * voice instruction [SpeechAnnouncement] including the synthesized speech mp3 file
     * from Mapbox's API Voice.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param consumer is a [SpeechValue] including the announcement to be played when the
     * announcement is ready or a [SpeechError] including the error information and a fallback
     * with the raw announcement (without file) that can be played with a text-to-speech engine.
     * @see [cancel]
     */
    fun generate(
        voiceInstruction: VoiceInstructions,
        consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
    ) {
        mainJobController.scope.launch {
            retrieveVoiceFile(voiceInstruction, consumer)
        }
    }

    /**
     * The method stops the process of retrieving the file voice instruction [SpeechAnnouncement]
     * and destroys any related callbacks.
     * @see [generate]
     */
    fun cancel() {
        mainJobController.job.children.forEach {
            it.cancel()
        }
    }

    /**
     * Given the [SpeechAnnouncement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    fun clean(announcement: SpeechAnnouncement) {
        voiceAPI.clean(announcement)
    }

    private suspend fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
        consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
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
                consumer.accept(
                    ExpectedFactory.createValue(
                        SpeechValue(
                            // Can't be null as it's checked in retrieveVoiceFile
                            SpeechAnnouncement.Builder(announcement!!).apply {
                                ssmlAnnouncement(ssmlAnnouncement)
                                file(result.instructionFile)
                            }.build()
                        )
                    )
                )
            }
            is VoiceState.VoiceError -> {
                processVoiceAnnouncement(
                    voiceInstruction
                ) { available ->
                    consumer.accept(
                        ExpectedFactory.createError(
                            SpeechError(
                                result.exception,
                                null,
                                available
                            )
                        )
                    )
                }
            }
        }
    }

    private suspend fun processVoiceAnnouncement(
        voiceInstruction: VoiceInstructions,
        onAvailable: (SpeechAnnouncement) -> Unit
    ) {
        val checkVoiceInstructionsResult =
            VoiceProcessor.process(VoiceAction.PrepareTypeAndAnnouncement(voiceInstruction))
        when (checkVoiceInstructionsResult as VoiceResult.VoiceTypeAndAnnouncement) {
            is VoiceResult.VoiceTypeAndAnnouncement.Success -> {
                val announcement = voiceInstruction.announcement()
                val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                // Can't be null as it's checked in processVoiceAnnouncement
                val available = SpeechAnnouncement.Builder(announcement!!)
                    .ssmlAnnouncement(ssmlAnnouncement)
                    .build()
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
