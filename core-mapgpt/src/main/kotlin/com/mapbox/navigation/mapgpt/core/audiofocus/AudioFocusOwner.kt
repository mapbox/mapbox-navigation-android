package com.mapbox.navigation.mapgpt.core.audiofocus

/**
 * Specifies the owner for the audio focus
 */
sealed class AudioFocusOwner {

    /**
     * The focus is requested for playing an instruction with the media player.
     */
    object MediaPlayer : AudioFocusOwner() {
        override fun toString(): String = "MediaPlayer"
    }

    /**
     * The focus is requested for playing an instruction using text-to-speech service.
     */
    object TextToSpeech : AudioFocusOwner() {
        override fun toString(): String = "TextToSpeech"
    }

    /**
     * The focus is requested for using the speech-to-text service.
     */
    object SpeechToText : AudioFocusOwner() {
        override fun toString(): String = "SpeechToText"
    }
}
