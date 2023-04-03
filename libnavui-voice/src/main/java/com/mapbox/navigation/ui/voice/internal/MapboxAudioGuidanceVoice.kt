package com.mapbox.navigation.ui.voice.internal

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsPrefetcher
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Controls voice guidance for the car.
 *
 * @param mapboxSpeechApi instance of [MapboxSpeechApi]
 * @param mapboxVoiceInstructionsPlayer stream of [VoiceInstructions].
 */
class MapboxAudioGuidanceVoice(
    internal val mapboxSpeechApi: MapboxSpeechApi,
    private val mapboxVoiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
) {
    /**
     * Load and play [VoiceInstructions].
     * This method will suspend until announcement finishes playback.
     */
    suspend fun speak(voiceInstructions: VoiceInstructions?): SpeechAnnouncement? {
        return speakInternal(voiceInstructions, MapboxSpeechApi::generate)
    }

    /**
     * Play [VoiceInstructions]. Take the file predownloaded via [VoiceInstructionsPrefetcher] or,
     * if absent, use onboard TTS engine.
     * This method will suspend until announcement finishes playback.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    suspend fun speakPredownloaded(voiceInstructions: VoiceInstructions?): SpeechAnnouncement? {
        return speakInternal(voiceInstructions, MapboxSpeechApi::generatePredownloaded)
    }

    private suspend fun speakInternal(
        voiceInstructions: VoiceInstructions?,
        generateBlock: MapboxSpeechApi.(
            VoiceInstructions,
            MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
        ) -> Unit
    ): SpeechAnnouncement? {
        return if (voiceInstructions != null) {
            val announcement = mapboxSpeechApi.generateAsync(voiceInstructions, generateBlock)
            try {
                mapboxVoiceInstructionsPlayer.play(announcement)
                announcement
            } finally {
                withContext(NonCancellable) {
                    mapboxSpeechApi.clean(announcement)
                }
            }
        } else {
            mapboxSpeechApi.cancel()
            mapboxVoiceInstructionsPlayer.clear()
            null
        }
    }

    private suspend fun MapboxSpeechApi.generateAsync(
        instructions: VoiceInstructions,
        generateBlock: MapboxSpeechApi.(
            VoiceInstructions,
            MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
        ) -> Unit
    ): SpeechAnnouncement = suspendCancellableCoroutine { cont ->
        generateBlock(
            instructions,
            MapboxNavigationConsumer { value ->
                val announcement = value.value?.announcement ?: value.error!!.fallback
                cont.resume(announcement)
            }
        )
        cont.invokeOnCancellation { cancel() }
    }

    private suspend fun MapboxVoiceInstructionsPlayer.play(
        announcement: SpeechAnnouncement
    ): SpeechAnnouncement = suspendCancellableCoroutine { cont ->
        play(announcement) {
            cont.resume(announcement)
        }
        cont.invokeOnCancellation { clear() }
    }
}
