package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import android.os.Build

internal object AudioFocusDelegateProvider {

    fun retrieveAudioFocusDelegate(
        audioManager: AudioManager,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): AudioFocusDelegate {
        return buildAudioFocusDelegate(audioManager, playerAttributes)
    }

    private fun buildAudioFocusDelegate(
        audioManager: AudioManager,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): AudioFocusDelegate {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoAndLaterAudioFocusDelegate(audioManager, playerAttributes)
        } else PreOreoAudioFocusDelegate(audioManager, playerAttributes)
    }
}
