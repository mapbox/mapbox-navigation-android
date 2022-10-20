package com.mapbox.navigation.ui.voice.api

import android.content.Context

internal object VoiceInstructionsFilePlayerProvider {

    fun retrieveVoiceInstructionsFilePlayer(
        context: Context,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): VoiceInstructionsFilePlayer = VoiceInstructionsFilePlayer(
        context,
        playerAttributes,
    )
}
