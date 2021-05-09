package com.mapbox.navigation.ui.voice.api

import android.content.Context

internal object VoiceInstructionsFilePlayerProvider {

    fun retrieveVoiceInstructionsFilePlayer(
        context: Context,
        accessToken: String,
        language: String,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): VoiceInstructionsFilePlayer = VoiceInstructionsFilePlayer(
        context,
        accessToken,
        language,
        playerAttributes,
    )
}
