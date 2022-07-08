package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Mapbox Speech Api that allows you to generate an announcement based on [VoiceInstructions]
 * @param context Context
 * @param accessToken String
 * @param language [Locale] language (IETF BCP 47)
 * @param options [MapboxSpeechApiOptions] (optional)
 */
class MapboxSpeechApi @JvmOverloads constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: MapboxSpeechApiOptions = MapboxSpeechApiOptions.Builder().build()
) {

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
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
        voiceAPI.cancel()
    }

    /**
     * Given the [SpeechAnnouncement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    fun clean(announcement: SpeechAnnouncement) {
        voiceAPI.clean(announcement)
    }

    @Throws(IllegalStateException::class)
    private suspend fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
        consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
    ) {
        when (val result = voiceAPI.retrieveVoiceFile(voiceInstruction)) {
            is VoiceState.VoiceFile -> {
                val announcement = voiceInstruction.announcement()
                val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                consumer.accept(
                    ExpectedFactory.createValue(
                        SpeechValue(
                            // Can't be null as it's checked in retrieveVoiceFile
                            SpeechAnnouncement.Builder(announcement!!)
                                .ssmlAnnouncement(ssmlAnnouncement)
                                .file(result.instructionFile)
                                .build()
                        )
                    )
                )
            }
            is VoiceState.VoiceError -> {
                val fallback = getFallbackAnnouncement(voiceInstruction)
                val speechError = SpeechError(result.exception, null, fallback)
                consumer.accept(ExpectedFactory.createError(speechError))
            }
        }
    }

    @Throws(IllegalStateException::class)
    private fun getFallbackAnnouncement(voiceInstruction: VoiceInstructions): SpeechAnnouncement {
        VoiceInstructionsParser.parse(voiceInstruction).error?.also {
            throw IllegalStateException(it.message)
        }

        val announcement = voiceInstruction.announcement()
        val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
        // Can't be null as it's checked in VoiceInstructionsParser.parse
        return SpeechAnnouncement.Builder(announcement!!)
            .ssmlAnnouncement(ssmlAnnouncement)
            .build()
    }
}
