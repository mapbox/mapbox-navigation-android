package com.mapbox.navigation.ui.voice.api

/**
 * Interface definition for a callback to be invoked when a player requests audio focus
 */
fun interface AudioFocusRequestCallback {

    /**
     * Invoked when the AudioFocusDelegate processed request.
     * @param result returns true on successful focus change request.
     */
    operator fun invoke(result: Boolean)
}
