package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Interface definition for a callback to be invoked when a voice instruction is retrieved.
 */
interface SpeechCallback {

    /**
     * Invoked when the announcement is ready.
     * @param state represents the announcement to be played.
     */
    fun onAvailable(state: SpeechState.Speech.Available)

    /**
     * Invoked when there is an error playing the voice instruction.
     * @param error error message.
     */
    fun onError(error: SpeechState.Speech.Error)
}
