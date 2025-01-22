package com.mapbox.navigation.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.VoiceState
import java.io.File

/**
 * An Api that allows you to retrieve voice instruction files based on [VoiceInstructions]
 */
internal interface VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceState]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     */
    suspend fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
    ): VoiceState

    /**
     * Given the [SpeechAnnouncement] the method may cleanup any associated files
     * previously generated.
     * @param announcement
     */
    fun clean(announcement: SpeechAnnouncement)
}
