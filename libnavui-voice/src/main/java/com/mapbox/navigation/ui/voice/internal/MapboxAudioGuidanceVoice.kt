package com.mapbox.navigation.ui.voice.internal

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
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
     * Load and play [SpeechAnnouncement].
     * This method will suspend until announcement finishes playback.
     */
    suspend fun speak(voiceInstructions: VoiceInstructions?): SpeechAnnouncement? {
        return if (voiceInstructions != null) {
            val announcement = mapboxSpeechApi.generate(voiceInstructions)
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

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private suspend fun MapboxSpeechApi.generate(
        instructions: VoiceInstructions
    ): SpeechAnnouncement = suspendCancellableCoroutine { cont ->
        generatePredownloaded(instructions) { value ->
            val announcement = value.value?.announcement ?: value.error!!.fallback
            cont.resume(announcement)
        }
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
