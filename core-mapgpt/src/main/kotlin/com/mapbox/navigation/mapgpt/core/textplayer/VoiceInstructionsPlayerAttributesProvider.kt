package com.mapbox.navigation.mapgpt.core.textplayer

import android.media.AudioAttributes
import android.os.Build
import com.mapbox.navigation.mapgpt.core.textplayer.options.PlayerOptions

internal object VoiceInstructionsPlayerAttributesProvider {

    fun retrievePlayerAttributes(
        options: PlayerOptions,
    ): PlayerAttributes {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PlayerAttributes.OreoAndLaterAttributes(
                options,
                AudioAttributes.Builder(),
            )
        } else {
            PlayerAttributes.PreOreoAttributes(options)
        }
    }
}
