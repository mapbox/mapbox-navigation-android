package com.mapbox.navigation.voice.api

/**
 * An Api that allows you to interact with the audio focus
 */
interface AudioFocusDelegate {

    /**
     * Request audio focus. Send a request to obtain the audio focus
     * @return true on successful focus change request.
     */
    fun requestFocus(): Boolean

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive focus.
     * @return true on successful focus change request.
     */
    fun abandonFocus(): Boolean
}
