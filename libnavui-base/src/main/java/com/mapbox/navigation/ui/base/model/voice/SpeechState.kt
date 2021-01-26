package com.mapbox.navigation.ui.base.model.voice

import androidx.annotation.FloatRange
import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the speech player data.
 */
sealed class SpeechState : MapboxState {

    /**
     * The structure represents different state for a Speech.
     */
    sealed class Speech : SpeechState() {

        /**
         * The state is returned when the speech is ready to be played on the UI.
         * @property announcement
         */
        data class Available(val announcement: Announcement) : Speech()

        /**
         * The state is returned if there is an error playing the voice instruction
         * @property exception String error message
         */
        data class Error(val exception: String) : Speech()
    }

    /**
     * The state is returned if the voice instruction is ready to be played.
     * @property announcement
     */
    data class ReadyToPlay(val announcement: Announcement) : SpeechState()

    /**
     * The state is returned if the voice instruction is done playing.
     * @property announcement
     */
    data class DonePlaying(val announcement: Announcement) : SpeechState()

    /**
     * The state is returned if we change the speech volume.
     * @property level
     */
    data class Volume(@FloatRange(from = 0.0, to = 1.0) val level: Float) : SpeechState()
}
