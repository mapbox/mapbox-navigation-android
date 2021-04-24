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

    private val audioFocusRequest: AudioFocusRequest = AudioFocusRequest.Builder(options.focusGain)
        .setAudioAttributes(options.audioAttributes)
        .build()

    override fun requestFocus(): Boolean {
        return when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> true
            else -> false
        }
    }

    override fun abandonFocus(): Boolean {
        return when (audioManager.abandonAudioFocusRequest(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
            else -> false
        }
    }
}
