package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Interface definition for a callback to be invoked when a voice instruction is played.
 */
interface VoiceInstructionsPlayerCallback {

    /**
     * Invoked when the speech player is done playing.
     * @param state represents that the speech player is done playing
     */
    fun onDone(state: SpeechState.DonePlaying)
}
