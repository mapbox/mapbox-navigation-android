package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

internal object VoiceInstructionsFilePlayerProvider {

    fun retrieveVoiceInstructionsFilePlayer(
        context: Context,
        accessToken: String,
        language: String,
        options: VoiceInstructionsPlayerOptions,
    ): VoiceInstructionsFilePlayer = VoiceInstructionsFilePlayer(
        context,
        accessToken,
        language,
        options,
    )
}
