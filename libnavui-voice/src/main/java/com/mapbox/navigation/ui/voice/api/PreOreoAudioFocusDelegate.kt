package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager

internal class PreOreoAudioFocusDelegate(
    private val audioManager: AudioManager
) : AudioFocusDelegate {

    override fun requestFocus() {
        audioManager.requestAudioFocus(
            null, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
    }

    override fun abandonFocus() {
        audioManager.abandonAudioFocus(null)
    }
}
