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

    private fun buildAudioFocusRequest(playerAttributes: VoiceInstructionsPlayerAttributes) = AudioFocusRequest
        .Builder(playerAttributes.options.focusGain)
        .apply { playerAttributes.applyOn(this) }
        .build()
}
