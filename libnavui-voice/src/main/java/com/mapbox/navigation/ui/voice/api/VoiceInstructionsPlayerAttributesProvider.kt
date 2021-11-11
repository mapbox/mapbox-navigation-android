package com.mapbox.navigation.ui.voice.api

import android.media.AudioAttributes
import android.os.Build
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal object VoiceInstructionsPlayerAttributesProvider {

    fun retrievePlayerAttributes(
        options: VoiceInstructionsPlayerOptions,
    ): VoiceInstructionsPlayerAttributes {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
                options,
                AudioAttributes.Builder()
            )
        } else {
            VoiceInstructionsPlayerAttributes.PreOreoAttributes(options)
        }
    }
}
