package com.mapbox.navigation.mapgpt.core.audiofocus

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.mapbox.navigation.mapgpt.core.textplayer.VoiceInstructionsPlayerAttributesProvider
import com.mapbox.navigation.mapgpt.core.textplayer.options.PlayerOptions

/**
 * Factory for creating default instance of AsyncAudioFocusDelegate.
 */
internal object AudioFocusDelegateProvider {

    /**
     * Create a default instance of AsyncAudioFocusDelegate.
     *
     * @param context Context
     * @param options PlayerOptions
     * @return [AudioFocusManager] instance that works for different Android versions.
     */
    fun defaultAudioFocusDelegate(
        context: Context,
        options: PlayerOptions,
    ): AudioFocusManager {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val playerAttributes = VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(
            options,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OreoAndLaterAudioFocusManager(audioManager, playerAttributes)
        } else {
            PreOreoAudioFocusManager(audioManager, playerAttributes)
        }
    }
}
