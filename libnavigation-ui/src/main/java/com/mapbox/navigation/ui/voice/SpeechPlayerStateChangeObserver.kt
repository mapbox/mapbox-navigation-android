package com.mapbox.navigation.ui.voice

/**
 * Interface definition for an observer that get's notified
 * whenever the [SpeechPlayer]'s [SpeechPlayerState] changes.
 */
internal interface SpeechPlayerStateChangeObserver {

    /**
     * Invoked whenever the [SpeechPlayer]'s [SpeechPlayerState] changes.
     *
     * @param state the latest SpeechPlayer state
     */
    fun onStateChange(state: SpeechPlayerState)
}
