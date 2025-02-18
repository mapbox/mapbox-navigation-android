package com.mapbox.navigation.mapgpt.core.audiofocus

import android.media.AudioManager
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerAttributes

internal class PreOreoAudioFocusManager(
    private val audioManager: AudioManager,
    private val playerAttributes: PlayerAttributes,
) : AudioFocusManager {

    override fun request(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        audioFocusRequestHolder.add(owner)
        val streamType = when (owner) {
            AudioFocusOwner.MediaPlayer -> playerAttributes.options.streamType
            AudioFocusOwner.TextToSpeech -> playerAttributes.options.ttsStreamType
            AudioFocusOwner.SpeechToText -> playerAttributes.options.streamType
        }
        val result = audioManager.requestAudioFocus(
            null,
            streamType,
            playerAttributes.options.focusGain,
        )
        callback(
            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED,
                -> true

                else -> false
            },
        )
    }

    override fun abandon(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        if (!audioFocusRequestHolder.hasRequestedFocus(owner)) {
            callback(true)
            return
        }
        callback(
            when (audioManager.abandonAudioFocus(null)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    audioFocusRequestHolder.remove(owner)
                    true
                }

                else -> false
            },
        )
    }

    private companion object {

        private val audioFocusRequestHolder = AudioFocusRequestHolder()
    }
}
