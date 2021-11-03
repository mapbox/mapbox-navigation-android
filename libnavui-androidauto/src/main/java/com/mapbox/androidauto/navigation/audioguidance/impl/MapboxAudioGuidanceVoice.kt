package com.mapbox.androidauto.navigation.audioguidance.impl

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Controls voice guidance for the car.
 *
 * @param mapboxSpeechApi language (ISO 639)
 * @param mapboxVoiceInstructionsPlayer stream of [VoiceInstructions].
 */
class MapboxAudioGuidanceVoice(
    private val mapboxSpeechApi: MapboxSpeechApi,
    private val mapboxVoiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
) {
    fun speak(voiceInstructions: VoiceInstructions?): Flow<SpeechAnnouncement?> {
        return if (voiceInstructions != null) {
            speechFlow(voiceInstructions)
        } else {
            mapboxSpeechApi.cancel()
            mapboxVoiceInstructionsPlayer.clear()
            flowOf(null)
        }
    }

    private fun speechFlow(voiceInstructions: VoiceInstructions): Flow<SpeechAnnouncement> = callbackFlow {
        val speechCallback = MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { value ->
            val speechAnnouncement = value.value?.announcement ?: value.error!!.fallback
            mapboxVoiceInstructionsPlayer.play(speechAnnouncement) {
                mapboxSpeechApi.clean(it)
                trySend(speechAnnouncement).onSuccess {
                    close()
                }.onFailure {
                    close()
                }
            }
        }
        mapboxSpeechApi.generate(voiceInstructions, speechCallback)
        awaitClose {
            mapboxSpeechApi.cancel()
            mapboxVoiceInstructionsPlayer.clear()
        }
    }
}
