package com.mapbox.navigation.ui.voice

/**
 * The [SpeechPlayer] will be at one of these states.
 */
internal enum class SpeechPlayerState {
    /**
     * The offline player [AndroidSpeechPlayer] is initializing.
     * In this state, can't use the [AndroidSpeechPlayer]. Only [MapboxSpeechPlayer] can be used.
     */
    OFFLINE_PLAYER_INITIALIZING,

    /**
     * The offline player [AndroidSpeechPlayer] is ready for use.
     * This state will be only fired once.
     */
    OFFLINE_PLAYER_INITIALIZED,

    /**
     * No voice guidance is playing. [SpeechPlayer] is idle.
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
