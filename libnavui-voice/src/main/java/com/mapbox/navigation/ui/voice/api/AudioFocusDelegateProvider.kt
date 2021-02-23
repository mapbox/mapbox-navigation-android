package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import android.os.Build
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal object AudioFocusDelegateProvider {

    fun retrieveAudioFocusDelegate(
        audioManager: AudioManager,
        options: VoiceInstructionsPlayerOptions
    ): AudioFocusDelegate {
        return buildAudioFocusDelegate(audioManager, options)
    }

    private fun buildAudioFocusDelegate(
        audioManager: AudioManager,
        options: VoiceInstructionsPlayerOptions
    ): AudioFocusDelegate {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoAndLaterAudioFocusDelegate(audioManager, options)
        } else PreOreoAudioFocusDelegate(audioManager, options)
    }
}
