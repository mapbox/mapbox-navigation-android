package com.mapbox.navigation.voice.model

/**
 * The state is returned if there is an error retrieving the voice instruction
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 * @param fallback represents the raw announcement (without file)
 * that can be played with a text-to-speech engine.
 */
class SpeechError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?,
    val fallback: SpeechAnnouncement,
)
