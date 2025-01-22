package com.mapbox.navigation.voice.api

import android.content.Context

internal object VoiceInstructionsTextPlayerProvider {

    fun retrieveVoiceInstructionsTextPlayer(
        context: Context,
        language: String,
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): VoiceInstructionsTextPlayer = VoiceInstructionsTextPlayer(
        context,
        language,
        playerAttributes,
    )
}
