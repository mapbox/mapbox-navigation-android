package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val options: VoiceInstructionsPlayerOptions
) : AudioFocusDelegate {

    override fun requestFocus() {
        audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            options.focusGain
        )
    }

    override fun abandonFocus() {
        audioManager.abandonAudioFocus(null)
    }
}
