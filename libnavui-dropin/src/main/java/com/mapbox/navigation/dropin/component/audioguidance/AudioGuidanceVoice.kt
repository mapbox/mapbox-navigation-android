package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Controls audio guidance one instruction at a time.
 *
 * @param mapboxSpeechApi language (ISO 639)
 * @param mapboxVoiceInstructionsPlayer stream of [VoiceInstructions].
 */
internal class AudioGuidanceVoice(
    private val mapboxSpeechApi: MapboxSpeechApi,
    private val mapboxVoiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun speak(voiceInstructions: VoiceInstructions): Flow<SpeechAnnouncement?> =
        callbackFlow {
            val speechCallback =
                MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { value ->
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
