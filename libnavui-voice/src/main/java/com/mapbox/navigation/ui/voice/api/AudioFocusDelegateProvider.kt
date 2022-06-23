package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal object AudioFocusDelegateProvider {

    fun defaultAudioFocusDelegate(
        context: Context,
        options: VoiceInstructionsPlayerOptions,
    ) = createAudioFocusDelegate(
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(options)
    )

    fun createAudioFocusDelegate(
        audioManager: AudioManager,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): AsyncAudioFocusDelegate {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoAndLaterAudioFocusDelegate(audioManager, playerAttributes)
        } else {
            PreOreoAudioFocusDelegate(audioManager, playerAttributes)
        }
    }
}
