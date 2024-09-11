package com.mapbox.navigation.voice.api

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.navigation.voice.model.AudioFocusOwner

@RequiresApi(api = Build.VERSION_CODES.O)
internal class OreoAndLaterAudioFocusDelegate(
    private val audioManager: AudioManager,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : AsyncAudioFocusDelegate {

    private var audioFocusOwner: AudioFocusOwner = AudioFocusOwner.MediaPlayer

    override fun requestFocus(
        owner: AudioFocusOwner,
        callback: AudioFocusRequestCallback,
    ) {
        audioFocusOwner = owner
        val audioFocusRequest = buildAudioFocusRequest(audioFocusOwner, playerAttributes)
        callback(
            when (audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED,
                -> true
                else -> false
            },
        )
    }

    override fun abandonFocus(callback: AudioFocusRequestCallback) {
        val audioFocusRequest = buildAudioFocusRequest(audioFocusOwner, playerAttributes)
        callback(
            when (audioManager.abandonAudioFocusRequest(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
                else -> false
            },
        )
    }

    private fun buildAudioFocusRequest(
        owner: AudioFocusOwner,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ) = AudioFocusRequest
        .Builder(playerAttributes.options.focusGain)
        .apply { playerAttributes.applyOn(owner, this) }
        .build()
}
