package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal object VoiceInstructionsTextPlayerProvider {

    fun retrieveVoiceInstructionsTextPlayer(
        context: Context,
        language: String,
        options: VoiceInstructionsPlayerOptions,
    ): VoiceInstructionsTextPlayer = VoiceInstructionsTextPlayer(
        context,
        language,
        options,
    )
}
