package com.mapbox.navigation.ui.voice.api

/**
 * An Api that allows you to interact with the audio focus
 */
interface AudioFocusDelegate {

    /**
     * Request audio focus. Send a request to obtain the audio focus
     * @param callback invoked when a delegate processed audio request
     */
    fun requestFocus(callback: AudioFocusRequestCallback)

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive focus.
     * @param callback invoked when a delegate processed audio request
     */
    fun abandonFocus(callback: AudioFocusRequestCallback)
}
