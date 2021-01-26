package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.SpeechState

internal data class PlayCallback(
    val announcement: SpeechState.ReadyToPlay,
    val callback: VoiceInstructionsPlayerCallback
)
