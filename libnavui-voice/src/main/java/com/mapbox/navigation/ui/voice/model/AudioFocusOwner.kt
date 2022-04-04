package com.mapbox.navigation.ui.voice.model

/**
 * Specifies the owner for the audio focus
 */
enum class AudioFocusOwner {

    /**
     * The focus is requsted for playing an instruction with the media player.
     */
    MediaPlayer,

    /**
     * The focus is requsted for playing an instruction using text-to-speach service.
     */
    TextToSpeech,
}
