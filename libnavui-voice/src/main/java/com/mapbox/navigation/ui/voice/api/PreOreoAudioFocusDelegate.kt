package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val options: VoiceInstructionsPlayerOptions
) : AudioFocusDelegate {

    override fun requestFocus(): Boolean {
        return audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            options.focusGain
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonFocus() {
        audioManager.abandonAudioFocus(null)
    }
}
