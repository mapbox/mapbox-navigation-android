package com.mapbox.navigation.ui.voice.api

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

@RequiresApi(api = Build.VERSION_CODES.O)
internal class OreoAndLaterAudioFocusDelegate(
    private val audioManager: AudioManager,
    options: VoiceInstructionsPlayerOptions
) : AudioFocusDelegate {

    private val audioFocusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(options.focusGain).build()

    override fun requestFocus(): Boolean {
        return audioManager.requestAudioFocus(audioFocusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }
}
