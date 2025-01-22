package com.mapbox.navigation.voice.api

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions

/**
 * Factory for creating default instance of AsyncAudioFocusDelegate.
 */
object AudioFocusDelegateProvider {

    /**
     * Create a default instance of AsyncAudioFocusDelegate.
     *
     * @param context Context
     * @param options VoiceInstructionsPlayerOptions
     * @return AsyncAudioFocusDelegate instance
     */
    fun defaultAudioFocusDelegate(
        context: Context,
        options: VoiceInstructionsPlayerOptions,
    ): AsyncAudioFocusDelegate = defaultAudioFocusDelegate(
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(options),
    )

    /**
     * Create a default instance of AsyncAudioFocusDelegate.
     *
     * @param audioManager AudioManager
     * @param playerAttributes VoiceInstructionsPlayerAttributes
     * @return AsyncAudioFocusDelegate instance
     */
    fun defaultAudioFocusDelegate(
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
