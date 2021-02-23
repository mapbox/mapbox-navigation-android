package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
import android.os.Build

internal object AudioFocusDelegateProvider {

    fun retrieveAudioFocusDelegate(audioManager: AudioManager): AudioFocusDelegate {
        return buildAudioFocusDelegate(audioManager)
    }

    private fun buildAudioFocusDelegate(audioManager: AudioManager): AudioFocusDelegate {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoAndLaterAudioFocusDelegate(audioManager)
        } else PreOreoAudioFocusDelegate(audioManager)
    }
}
