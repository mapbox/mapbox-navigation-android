package com.mapbox.navigation.voice.api

import com.mapbox.navigation.voice.model.SpeechAnnouncement

/**
 * Interface definition for a callback to be invoked when a voice instruction is played.
 */
internal fun interface VoiceInstructionsPlayerCallback {

    /**
     * Invoked when the speech player is done playing.
     * @param announcement represents that the speech player is done playing
     */
    fun onDone(announcement: SpeechAnnouncement)
}
