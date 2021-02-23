package com.mapbox.navigation.ui.voice.api

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.O)
internal class OreoAndLaterAudioFocusDelegate(
    private val audioManager: AudioManager
) : AudioFocusDelegate {

    private val audioFocusRequest: AudioFocusRequest = AudioFocusRequest.Builder(FOCUS_GAIN).build()

    override fun requestFocus() {
        audioManager.requestAudioFocus(audioFocusRequest)
    }

    override fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private companion object {
        private const val FOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
    }
}
