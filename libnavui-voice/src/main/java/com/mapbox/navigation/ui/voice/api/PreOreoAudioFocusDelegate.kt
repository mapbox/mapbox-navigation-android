package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val options: VoiceInstructionsPlayerOptions
) : AudioFocusDelegate {

    override fun requestFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            null,
            options.playerAttributes.streamType,
            options.focusGain
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
