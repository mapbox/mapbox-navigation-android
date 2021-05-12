package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : AudioFocusDelegate {

    override fun requestFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            null,
            playerAttributes.options.streamType,
            playerAttributes.options.focusGain
        )
        return when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> true
            else -> false
        }
    }

    override fun abandonFocus(): Boolean {
        return when (audioManager.abandonAudioFocus(null)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
            else -> false
        }
    }
}
