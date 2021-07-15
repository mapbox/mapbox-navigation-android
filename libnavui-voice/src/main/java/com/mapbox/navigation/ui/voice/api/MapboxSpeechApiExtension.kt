package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue

/**
 * Extension functions for [MapboxSpeechApi] calls that are implemented as callbacks. This offers
 * an alternative to those callbacks by providing Kotlin oriented suspend functions.
 */
object MapboxSpeechApiExtension {

    /**
     * Given [VoiceInstructions] the method will try to generate the
     * voice instruction [SpeechAnnouncement] including the synthesized speech mp3 file
     * from Mapbox's API Voice.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @return a state which contains the side effects to be played when the
     * announcement is ready or the error information and a fallback
     * with the raw announcement (without file) that can be played with a text-to-speech engine.
     */
    suspend fun MapboxSpeechApi.generate(
        voiceInstruction: VoiceInstructions,
        block: (Expected<SpeechValue, SpeechError>) -> Unit,
    ) {
        retrieveVoiceFile(
            voiceInstruction,
            object : MapboxNavigationConsumer<Expected<SpeechValue, SpeechError>> {
                override fun accept(value: Expected<SpeechValue, SpeechError>) {
                    block(value)
                }
            })
    }
}
