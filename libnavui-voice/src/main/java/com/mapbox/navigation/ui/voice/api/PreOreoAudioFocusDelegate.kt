package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import com.mapbox.navigation.ui.voice.model.AudioFocusOwner

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : AsyncAudioFocusDelegate {

    override fun requestFocus(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        val streamType = when (owner) {
            AudioFocusOwner.MediaPlayer -> playerAttributes.options.streamType
            AudioFocusOwner.TextToSpeech -> playerAttributes.options.ttsStreamType
        }
        val result = audioManager.requestAudioFocus(
            null,
            streamType,
            playerAttributes.options.focusGain
        )
        callback(
            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> true
                else -> false
            }
        )
    }

    override fun abandonFocus(callback: AudioFocusRequestCallback) {
        callback(
            when (audioManager.abandonAudioFocus(null)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
                else -> false
            }
        )
    }
}
