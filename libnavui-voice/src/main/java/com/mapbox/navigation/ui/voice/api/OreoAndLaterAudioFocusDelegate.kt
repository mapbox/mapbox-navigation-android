package com.mapbox.navigation.ui.voice.api

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.O)
internal class OreoAndLaterAudioFocusDelegate(
    private val audioManager: AudioManager,
    playerAttributes: VoiceInstructionsPlayerAttributes,
) : AudioFocusDelegate {

    private val audioFocusRequest: AudioFocusRequest = buildAudioFocusRequest(playerAttributes)

    override fun requestFocus(callback: AudioFocusRequestCallback) {
        val result = when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> true
            else -> false
        }
        callback(result)
    }

    override fun abandonFocus(callback: AudioFocusRequestCallback) {
        val result = when (audioManager.abandonAudioFocusRequest(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
            else -> false
        }
        callback(result)
    }

    private fun buildAudioFocusRequest(
        playerAttributes: VoiceInstructionsPlayerAttributes
    ) = AudioFocusRequest
        .Builder(playerAttributes.options.focusGain)
        .apply { playerAttributes.applyOn(this) }
        .build()
}
