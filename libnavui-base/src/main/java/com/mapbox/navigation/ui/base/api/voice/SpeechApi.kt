package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.model.voice.Announcement

/**
 * An Api that allows you to generate an announcement based on [VoiceInstructions]
 */
interface SpeechApi {

    /**
     * Given [VoiceInstructions] the method will generate the voice instruction [Announcement].
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    fun generate(
        voiceInstruction: VoiceInstructions,
        callback: SpeechCallback
    )

    /**
     * The method stops the process of retrieving the voice instruction [Announcement]
     * and destroys any related callbacks.
     */
    fun cancel()

    /**
     * Given the [Announcement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    fun clean(announcement: Announcement)
}
