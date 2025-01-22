package com.mapbox.navigation.voice.model

/**
 * Specifies the owner for the audio focus
 */
enum class AudioFocusOwner {

    /**
     * The focus is requested for playing an instruction with the media player.
     */
    MediaPlayer,

    /**
     * The focus is requested for playing an instruction using text-to-speech service.
     */
    TextToSpeech,
}
