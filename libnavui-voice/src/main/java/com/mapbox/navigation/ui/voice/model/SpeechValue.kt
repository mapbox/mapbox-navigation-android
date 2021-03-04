package com.mapbox.navigation.ui.voice.model

/**
 * The state is returned when the speech is ready to be played on the UI.
 * It's also returned as a fallback when [SpeechApi.generate] fails.
 * In this case, the [File] from the [SpeechAnnouncement] will be null.
 * @param announcement
 */
class SpeechValue internal constructor(
    val announcement: SpeechAnnouncement
)
