package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement

internal data class PlayCallback(
    val announcement: SpeechAnnouncement,
    val callback: VoiceInstructionsPlayerCallback
)
