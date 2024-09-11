package com.mapbox.navigation.voice.model

import com.mapbox.navigation.voice.api.MapboxSpeechApi
import java.io.File

/**
 * The state is returned when the speech is ready to be played on the UI.
 * It's also returned as a fallback when [MapboxSpeechApi.generate] fails.
 * In this case, the [File] from the [SpeechAnnouncement] will be null.
 * @param announcement
 */
class SpeechValue internal constructor(
    val announcement: SpeechAnnouncement,
)
