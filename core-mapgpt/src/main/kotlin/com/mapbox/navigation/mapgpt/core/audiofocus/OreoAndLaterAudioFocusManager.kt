package com.mapbox.navigation.mapgpt.core.audiofocus

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerAttributes

@RequiresApi(api = Build.VERSION_CODES.O)
internal class OreoAndLaterAudioFocusManager(
    private val audioManager: AudioManager,
    private val playerAttributes: PlayerAttributes,
) : AudioFocusManager {

    override fun request(
        owner: AudioFocusOwner,
        callback: AudioFocusRequestCallback,
    ) {
        audioFocusRequestHolder.add(owner)
        val audioFocusRequest = buildAudioFocusRequest(owner, playerAttributes)
        callback(
            when (audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED,
                -> true

                else -> false
            },
        )
    }

    override fun abandon(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        if (!audioFocusRequestHolder.hasRequestedFocus(owner)) {
            callback(true)
            return
        }
        val audioFocusRequest = buildAudioFocusRequest(owner, playerAttributes)
        callback(
            when (audioManager.abandonAudioFocusRequest(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    audioFocusRequestHolder.remove(owner)
                    true
                }
                else -> false
            },
        )
    }

    private fun buildAudioFocusRequest(
        owner: AudioFocusOwner,
        playerAttributes: PlayerAttributes,
    ) = AudioFocusRequest
        .Builder(playerAttributes.options.focusGain)
        .apply { playerAttributes.applyOn(owner, this) }
        .build()

    private companion object {
        private val audioFocusRequestHolder = AudioFocusRequestHolder()
    }
}
