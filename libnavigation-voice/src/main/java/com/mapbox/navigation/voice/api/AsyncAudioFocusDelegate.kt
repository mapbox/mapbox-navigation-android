package com.mapbox.navigation.voice.api

import com.mapbox.navigation.voice.model.AudioFocusOwner

/**
 * An Api that allows you to interact with the audio focus in an asynchronous way.
 */
interface AsyncAudioFocusDelegate {
    /**
     * Request audio focus. Send a request to obtain the audio focus
     * @param owner specifies the owner for request
     * @param callback invoked when the delegate processed the audio request
     */
    fun requestFocus(owner: AudioFocusOwner, callback: AudioFocusRequestCallback)

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive the focus.
     * @param callback invoked when the delegate processed the audio request
     */
    fun abandonFocus(callback: AudioFocusRequestCallback)
}
