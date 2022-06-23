package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.model.VoiceState.VoiceError
import com.mapbox.navigation.ui.voice.model.VoiceState.VoiceFile
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
        return runCatching {
            val typeAndAnnouncement = VoiceInstructionsParser.parse(voiceInstruction).getOrThrow()
            val blob = speechProvider.load(typeAndAnnouncement).getOrThrow()
            val file = speechFileProvider.generateVoiceFileFrom(blob.inputStream())
            VoiceFile(file)
        }.getOrElse {
            VoiceError(it.localizedMessage ?: genericError(voiceInstruction))
        }
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

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        speechFileProvider.cancel()
    }

    private fun genericError(voiceInstruction: VoiceInstructions) =
        "Cannot load voice instructions $voiceInstruction"

    private fun <E : Throwable, V> Expected<E, V>.getOrThrow(): V {
        if (isError) throw error!!
        return value!!
    }
}
