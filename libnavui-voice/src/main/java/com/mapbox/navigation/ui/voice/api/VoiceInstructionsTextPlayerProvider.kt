package com.mapbox.navigation.ui.voice.api

import android.content.Context

internal object VoiceInstructionsTextPlayerProvider {

    fun retrieveVoiceInstructionsTextPlayer(
        context: Context,
        language: String
    ): VoiceInstructionsTextPlayer = VoiceInstructionsTextPlayer(context, language)
}
