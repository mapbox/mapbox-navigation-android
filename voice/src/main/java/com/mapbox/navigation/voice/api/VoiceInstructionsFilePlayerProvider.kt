package com.mapbox.navigation.voice.api

internal object VoiceInstructionsFilePlayerProvider {

    fun retrieveVoiceInstructionsFilePlayer(
        playerAttributes: VoiceInstructionsPlayerAttributes,
    ): VoiceInstructionsFilePlayer = VoiceInstructionsFilePlayer(
        playerAttributes,
    )
}
