package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : AudioFocusDelegate {

    override fun requestFocus(callback: AudioFocusRequestCallback) {
        val result = audioManager.requestAudioFocus(
            null,
            playerAttributes.options.streamType,
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
