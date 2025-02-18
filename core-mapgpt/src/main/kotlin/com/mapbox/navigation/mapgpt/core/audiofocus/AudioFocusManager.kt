package com.mapbox.navigation.mapgpt.core.audiofocus

/**
 * An Api that allows you to interact with the audio focus in an asynchronous way.
 */
interface AudioFocusManager {

    /**
     * Request audio focus. Send a request to obtain the audio focus
     * @param owner specifies the owner for request
     * @param callback invoked when the delegate processed the audio request
     */
    fun request(owner: AudioFocusOwner, callback: AudioFocusRequestCallback)

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive the focus.
     * @param owner specifies the owner for request
     * @param callback invoked when the delegate processed the audio request
     */
    fun abandon(owner: AudioFocusOwner, callback: AudioFocusRequestCallback)
}
