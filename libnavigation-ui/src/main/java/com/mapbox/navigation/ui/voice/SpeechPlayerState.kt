package com.mapbox.navigation.ui.voice

/**
 * The [SpeechPlayer] will be at one of these states.
 */
enum class SpeechPlayerState {
    /**
     * No voice guidance is playing. [SpeechPlayer] is not idle.
     */
    IDLE,

    /**
     * Voice guidance is playing through [AndroidSpeechPlayer].
     */
    OFFLINE_PLAYING,

    /**
     * Voice guidance is playing through [MapboxSpeechPlayer].
     */
    ONLINE_PLAYING
}
